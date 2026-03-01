package io.github.minehollow.zones.listener;

import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.zones.ZoneManager;
import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.event.ZoneEnterEvent;
import io.github.minehollow.zones.event.ZoneExitEvent;
import io.github.minehollow.zones.model.Zone;
import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

/**
 * Tracks zone transitions (enter/exit) per player and manages hide-players visibility.
 */
@RequiredArgsConstructor
public class ZoneMovementListener implements Listener {

    private final ZoneManager manager;
    private final ZoneQuery query;

    /** Current zone set per player — fastutil for O(1) amortized ops */
    private final Object2ObjectOpenHashMap<UUID, ObjectOpenHashSet<String>> playerZones = new Object2ObjectOpenHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!hasBlockChanged(e.getFrom(), e.getTo())) return;
        processTransition(e.getPlayer(), e.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        processTransition(e.getPlayer(), e.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        ObjectOpenHashSet<String> old = playerZones.remove(e.getPlayer().getUniqueId());
        if (old != null) {
            for (String id : old) {
                Zone zone = manager.getZone(id);
                if (zone != null) {
                    Bukkit.getPluginManager().callEvent(new ZoneExitEvent(e.getPlayer(), zone));
                }
            }
        }
    }

    private void processTransition(Player player, Location to) {
        List<Zone> current = manager.getZonesAt(to);
        ObjectOpenHashSet<String> currentIds = new ObjectOpenHashSet<>(current.size());
        for (int i = 0, len = current.size(); i < len; i++) currentIds.add(current.get(i).getId());

        ObjectOpenHashSet<String> previous = playerZones.getOrDefault(player.getUniqueId(), new ObjectOpenHashSet<>());

        // Entered zones
        for (Zone zone : current) {
            if (!previous.contains(zone.getId())) {
                var event = new ZoneEnterEvent(player, zone);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    currentIds.remove(zone.getId());
                }
            }
        }

        // Exited zones
        for (String oldId : previous) {
            if (!currentIds.contains(oldId)) {
                Zone zone = manager.getZone(oldId);
                if (zone != null) {
                    var event = new ZoneExitEvent(player, zone);
                    Bukkit.getPluginManager().callEvent(event);
                }
            }
        }

        playerZones.put(player.getUniqueId(), currentIds);

        // Handle hide-players visibility
        updateVisibility(player, to, current);
    }

    /**
     * If the player is in any zone with hide-players=ALLOW, hide all players
     * outside that zone and show players inside. Vice-versa for players outside.
     */
    private void updateVisibility(Player player, Location to, List<Zone> currentZones) {
        boolean inHiddenZone = false;
        ObjectOpenHashSet<String> hiddenZoneIds = new ObjectOpenHashSet<>(2);

        for (Zone zone : currentZones) {
            if (zone.getFlagState(ZoneFlag.HIDE_PLAYERS) == ZoneFlagState.ALLOW) {
                inHiddenZone = true;
                hiddenZoneIds.add(zone.getId());
            }
        }

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;

            if (inHiddenZone) {
                // Check if other player is in any of the same hidden zones
                boolean otherInSameZone = false;
                List<Zone> otherZones = manager.getZonesAt(other.getLocation());
                for (Zone oz : otherZones) {
                    if (hiddenZoneIds.contains(oz.getId())) {
                        otherInSameZone = true;
                        break;
                    }
                }

                if (otherInSameZone) {
                    player.showPlayer(BukkitPlatformPlugin.getInstance(), other);
                    other.showPlayer(BukkitPlatformPlugin.getInstance(), player);
                } else {
                    player.hidePlayer(BukkitPlatformPlugin.getInstance(), other);
                    other.hidePlayer(BukkitPlatformPlugin.getInstance(), player);
                }
            } else {
                player.showPlayer(BukkitPlatformPlugin.getInstance(), other);
            }
        }
    }

    /**
     * Returns the set of zone IDs the player is currently in.
     */
    public Set<String> getPlayerZones(UUID playerId) {
        return playerZones.getOrDefault(playerId, new ObjectOpenHashSet<>());
    }

    private boolean hasBlockChanged(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();
    }
}


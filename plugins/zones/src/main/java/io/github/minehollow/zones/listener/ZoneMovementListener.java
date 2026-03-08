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

import java.util.UUID;

/**
 * Tracks zone transitions (enter/exit) per player and manages hide-players visibility.
 * <p>
 * Zero-allocation on every tick:
 * <ul>
 *   <li>Uses {@link ZoneManager#forEachZoneAt} callbacks — never collects zones into a list.</li>
 *   <li>Stores a reusable {@link PlayerZoneState} per player (pre-allocated scratch array).</li>
 *   <li>Diffs old vs new zone IDs via direct array scan (stacks are 0–3 zones).</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ZoneMovementListener implements Listener {

    private static final String[] EMPTY_IDS = new String[0];
    private static final int MAX_ZONE_STACK = 8;

    private final ZoneManager manager;
    private final ZoneQuery query;

    /** Per-player zone state with reusable scratch buffer */
    private final Object2ObjectOpenHashMap<UUID, PlayerZoneState> playerStates = new Object2ObjectOpenHashMap<>();

    /**
     * Lightweight per-player tracking. The scratch array avoids allocation on every tick.
     */
    private static final class PlayerZoneState {
        /** Current zone IDs snapshot (immutable between transitions) */
        String[] currentIds = EMPTY_IDS;
        /** Reusable scratch buffer for building the new zone ID list */
        final String[] scratch = new String[MAX_ZONE_STACK];
        int scratchLen = 0;

        void resetScratch() { scratchLen = 0; }

        void pushScratch(String id) {
            if (scratchLen < scratch.length) scratch[scratchLen++] = id;
        }

        /** Commits scratch as the new currentIds (copies only the used portion) */
        void commit() {
            if (scratchLen == 0) {
                currentIds = EMPTY_IDS;
            } else {
                currentIds = new String[scratchLen];
                System.arraycopy(scratch, 0, currentIds, 0, scratchLen);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!hasBlockChanged(e.getFrom(), e.getTo())) return;
        if (processTransition(e.getPlayer(), e.getTo())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (processTransition(e.getPlayer(), e.getTo())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerZoneState state = playerStates.remove(e.getPlayer().getUniqueId());
        if (state != null) {
            for (int i = 0; i < state.currentIds.length; i++) {
                Zone zone = manager.getZone(state.currentIds[i]);
                if (zone != null) {
                    Bukkit.getPluginManager().callEvent(new ZoneExitEvent(e.getPlayer(), zone));
                }
            }
        }
    }

    /**
     * @return true if movement should be denied
     */
    private boolean processTransition(Player player, Location to) {
        UUID uuid = player.getUniqueId();
        PlayerZoneState state = playerStates.get(uuid);
        if (state == null) {
            state = new PlayerZoneState();
            playerStates.put(uuid, state);
        }

        String[] previousIds = state.currentIds;

        // Build new zone list into scratch buffer — zero allocation via forEachZoneAt
        state.resetScratch();
        final PlayerZoneState s = state; // effectively final for lambda
        manager.forEachZoneAt(to, zone -> s.pushScratch(zone.getId()));

        // Fast path: nothing changed
        if (arraysEqual(previousIds, state.scratch, state.scratchLen)) {
            return false;
        }

        // Check newly entered zones
        for (int i = 0; i < state.scratchLen; i++) {
            String id = state.scratch[i];
            if (!containsId(previousIds, id)) {
                Zone zone = manager.getZone(id);
                if (zone == null) continue;

                // Entry permission check
                if (zone.getFlagState(ZoneFlag.ENTRY) == ZoneFlagState.DENY
                    && !zone.isMember(uuid)
                    && !player.hasPermission("zones.entry." + id)) {
                    player.sendMessage("§cVocê não tem permissão para entrar na zona '" + zone.getDisplayName() + "'.");
                    return true;
                }

                var event = new ZoneEnterEvent(player, zone);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return true;
            }
        }

        // Check exited zones
        for (int i = 0; i < previousIds.length; i++) {
            if (!containsId(state.scratch, state.scratchLen, previousIds[i])) {
                Zone zone = manager.getZone(previousIds[i]);
                if (zone != null) {
                    Bukkit.getPluginManager().callEvent(new ZoneExitEvent(player, zone));
                }
            }
        }

        // Commit scratch → currentIds
        state.commit();

        // Visibility
        updateVisibility(player, state.currentIds);
        return false;
    }

    // ── Visibility ─────────────────────────────────────

    private void updateVisibility(Player player, String[] currentIds) {
        // Check if player is in any hidden zone
        boolean inHiddenZone = false;
        for (int i = 0; i < currentIds.length; i++) {
            Zone zone = manager.getZone(currentIds[i]);
            if (zone != null && zone.getFlagState(ZoneFlag.HIDE_PLAYERS) == ZoneFlagState.ALLOW) {
                inHiddenZone = true;
                break;
            }
        }

        var plugin = BukkitPlatformPlugin.getInstance();

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;

            if (inHiddenZone) {
                // Check if other shares any hidden zone — zero alloc via anyZoneAt
                boolean otherInSameHiddenZone = manager.anyZoneAt(other.getLocation(),
                    zone -> zone.getFlagState(ZoneFlag.HIDE_PLAYERS) == ZoneFlagState.ALLOW
                        && containsId(currentIds, zone.getId()));

                if (otherInSameHiddenZone) {
                    player.showPlayer(plugin, other);
                    other.showPlayer(plugin, player);
                } else {
                    player.hidePlayer(plugin, other);
                    other.hidePlayer(plugin, player);
                }
            } else {
                player.showPlayer(plugin, other);
            }
        }
    }

    // ── Array utils (no allocation) ────────────────────

    private static boolean containsId(String[] arr, String id) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(id)) return true;
        }
        return false;
    }

    private static boolean containsId(String[] arr, int len, String id) {
        for (int i = 0; i < len; i++) {
            if (arr[i].equals(id)) return true;
        }
        return false;
    }

    /** Compares a String[] against a scratch buffer (arr, len). */
    private static boolean arraysEqual(String[] prev, String[] scratch, int scratchLen) {
        if (prev.length != scratchLen) return false;
        for (int i = 0; i < scratchLen; i++) {
            if (!prev[i].equals(scratch[i])) return false;
        }
        return true;
    }

    /**
     * Returns the zone IDs the player is currently in. Returns a snapshot array directly.
     */
    public String[] getPlayerZoneIds(UUID playerId) {
        PlayerZoneState state = playerStates.get(playerId);
        return state != null ? state.currentIds : EMPTY_IDS;
    }

    private static boolean hasBlockChanged(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();
    }
}

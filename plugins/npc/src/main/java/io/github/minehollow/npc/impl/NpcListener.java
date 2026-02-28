package io.github.minehollow.npc.impl;

import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Manages NPC visibility based on render distance.
 * Uses block-level move threshold to reduce event overhead.
 */
public class NpcListener implements Listener {

    private final NpcRegistryImpl registry;

    public NpcListener(@NotNull NpcRegistryImpl registry) {
        this.registry = registry;
    }

    /**
     * Called after registration to handle players already online (e.g. plugin reload).
     */
    public void showToOnlinePlayers() {
        Tasks.runAsyncLater(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateVisibility(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // Delay to ensure the player is fully loaded and can receive packets
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                updateVisibility(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (NpcImpl npc : registry.getAllImpl()) {
            npc.removeViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        // Only check when the player moves a full block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        updateVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                updateVisibility(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                updateVisibility(event.getPlayer());
            }
        }, 5L);
    }

    private void updateVisibility(@NotNull Player player) {
        Location playerLoc = player.getLocation();
        for (NpcImpl npc : registry.getAllImpl()) {
            if (!npc.isSpawned()) continue;

            Location npcLoc = npc.getLocation();
            if (npcLoc.getWorld() == null || !npcLoc.getWorld().equals(playerLoc.getWorld())) {
                if (npc.isViewer(player.getUniqueId())) {
                    npc.removeViewer(player);
                }
                continue;
            }

            double distSq = npcLoc.distanceSquared(playerLoc);
            if (distSq <= npc.getRenderDistanceSq()) {
                if (!npc.isViewer(player.getUniqueId())) {
                    npc.addViewer(player);
                }
            } else {
                if (npc.isViewer(player.getUniqueId())) {
                    npc.removeViewer(player);
                }
            }
        }
    }
}


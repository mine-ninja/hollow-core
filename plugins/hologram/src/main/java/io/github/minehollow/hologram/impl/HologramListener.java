package io.github.minehollow.hologram.impl;

import io.github.minehollow.hologram.Hologram;
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
 * Manages hologram visibility based on render distance.
 * Uses block-level move threshold to reduce event overhead.
 */
public class HologramListener implements Listener {

    private final HologramRegistryImpl registry;

    public HologramListener(@NotNull HologramRegistryImpl registry) {
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
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                updateVisibility(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Hologram holo : registry.getAll()) {
            holo.removeViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
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
        double renderDistSq = registry.getRenderDistanceSq();

        for (Hologram holo : registry.getAll()) {
            if (!holo.isSpawned()) continue;

            Location holoLoc = holo.getLocation();
            if (holoLoc.getWorld() == null || !holoLoc.getWorld().equals(playerLoc.getWorld())) {
                if (holo.isViewer(player.getUniqueId())) {
                    holo.removeViewer(player);
                }
                continue;
            }

            double distSq = holoLoc.distanceSquared(playerLoc);
            if (distSq <= renderDistSq) {
                if (!holo.isViewer(player.getUniqueId())) {
                    holo.addViewer(player);
                }
            } else {
                if (holo.isViewer(player.getUniqueId())) {
                    holo.removeViewer(player);
                }
            }
        }
    }
}


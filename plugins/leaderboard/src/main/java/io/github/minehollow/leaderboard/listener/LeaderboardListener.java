package io.github.minehollow.leaderboard.listener;

import io.github.minehollow.leaderboard.LeaderboardManager;
import io.github.minehollow.minecraft.task.Tasks;
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
 * Handles hologram visibility for leaderboard displays.
 */
public class LeaderboardListener implements Listener {

    private final LeaderboardManager manager;

    public LeaderboardListener(@NotNull LeaderboardManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                manager.updateVisibility(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        manager.removeViewer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        manager.updateVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                manager.updateVisibility(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        Tasks.runAsyncLater(() -> {
            if (event.getPlayer().isOnline()) {
                manager.updateVisibility(event.getPlayer());
            }
        }, 5L);
    }
}


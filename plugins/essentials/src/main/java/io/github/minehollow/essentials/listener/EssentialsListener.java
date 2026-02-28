package io.github.minehollow.essentials.listener;

import io.github.minehollow.essentials.EssentialsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class EssentialsListener implements Listener {

    private final EssentialsPlugin plugin;

    public EssentialsListener(@NotNull EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Phase 1: Register the invisible tablist username BEFORE the server
     * sends ADD_PLAYER to other clients. Must be LOWEST so it runs first.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreJoin(@NotNull PlayerJoinEvent event) {
        plugin.getTabListManager().onPreJoin(event.getPlayer());
    }

    /**
     * Phase 2: Create teams and sync display names after the join is processed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        plugin.getTabListManager().onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        plugin.getTabListManager().onQuit(player);
        plugin.getHomeService().evict(uuid);
        plugin.getTpaService().cleanup(uuid);
        plugin.getTeleportService().cancel(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        // Only check block-level movement, not head rotation
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        plugin.getTeleportService().onPlayerMove(event.getPlayer());
    }
}


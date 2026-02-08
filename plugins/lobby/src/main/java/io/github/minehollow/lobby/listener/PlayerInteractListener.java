package io.github.minehollow.lobby.listener;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {
    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent e) {
        final var player = e.getPlayer();
        if (player.hasPermission("lobby.admin") && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        e.setCancelled(true);
    }
}
package io.github.minehollow.lobby.listener;

import io.github.minehollow.lobby.LobbyPlugin;
import io.github.minehollow.lobby.menu.ServerMenu;
import io.github.minehollow.minecraft.menu.MenuUtil;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {
    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent e) {
        final var player = e.getPlayer();

        final var item = e.getItem();
        if (item != null && item.isSimilar(LobbyPlugin.SERVER_SELECTOR)) {
            MenuUtil.openMenu(player, ServerMenu.class);
        }


        if (player.hasPermission("lobby.admin") && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        e.setCancelled(true);
    }
}
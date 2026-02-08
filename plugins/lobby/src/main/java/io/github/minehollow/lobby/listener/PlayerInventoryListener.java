package io.github.minehollow.lobby.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        this.cancelIfNotAdmin(e, (Player) e.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        this.cancelIfNotAdmin(e, (Player) e.getWhoClicked());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        this.cancelIfNotAdmin(e, e.getPlayer());
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        this.cancelIfNotAdmin(e, e.getPlayer());
    }


    private void cancelIfNotAdmin(Cancellable event, @NotNull Player player) {
        if (player.hasPermission("lobby.admin")) return;
        event.setCancelled(true);
    }
}
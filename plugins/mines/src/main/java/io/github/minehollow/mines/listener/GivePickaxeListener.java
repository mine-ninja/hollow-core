package io.github.minehollow.mines.listener;

import io.github.minehollow.mines.pickaxe.PickaxeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GivePickaxeListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            player.getInventory().addItem(PickaxeManager.createPickaxe(1));
        }
    }
}

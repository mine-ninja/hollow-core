package io.github.minehollow.kits.listener;

import io.github.minehollow.kits.KitService;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerJoinListener implements Listener {
    private final KitService kitService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        kitService.loadPlayerData(event.getPlayer().getUniqueId());
    }
}

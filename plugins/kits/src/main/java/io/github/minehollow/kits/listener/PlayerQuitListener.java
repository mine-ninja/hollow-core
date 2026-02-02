package io.github.minehollow.kits.listener;

import io.github.minehollow.kits.KitService;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PlayerQuitListener implements Listener {
    private final KitService kitService;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        kitService.evictPlayerCache(event.getPlayer().getUniqueId());
    }
}

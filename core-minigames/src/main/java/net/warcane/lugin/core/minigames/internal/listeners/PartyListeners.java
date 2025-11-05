package net.warcane.lugin.core.minigames.internal.listeners;

import net.warcane.lugin.core.minigames.MinigamesPlatform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record PartyListeners(MinigamesPlatform platform) implements Listener {

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event) {
        var player = event.getPlayer();
        platform.getPartyService().handlePlayerJoin(player);
    }

    @EventHandler
    public void onLeaveEvent(PlayerQuitEvent event) {
        var player = event.getPlayer();
        platform.getPartyService().handlePlayerLeave(player);
    }
}

package io.github.minehollow.minecraft.util.message.input;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class ChatInputEvents implements Listener {

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        if (!ChatInput.contains(event.getPlayer())) return;
        event.setCancelled(true);
        ChatInput.playerReply(event.getPlayer(), event.getMessage());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (!ChatInput.contains(event.getPlayer())) return;
        ChatInput.remove(event.getPlayer().getUniqueId());
    }
}

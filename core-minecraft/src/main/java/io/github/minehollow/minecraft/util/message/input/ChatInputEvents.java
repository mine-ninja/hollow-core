package io.github.minehollow.minecraft.util.message.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;


public class ChatInputEvents implements Listener {

//    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
//    public void onPlayerMessage(AsyncPlayerChatEvent event) {
//        if (!ChatInput.contains(event.getPlayer())) return;
//        event.setCancelled(true);
//        ChatInput.playerReply(event.getPlayer(), event.getMessage());
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMessage(AsyncChatEvent event) {
        if (!ChatInput.contains(event.getPlayer())) return;
        event.setCancelled(true);

        final var message = event.message();
        String msg = "cancelar";
        if (message instanceof TextComponent text) {
            msg = text.content();
        }

        ChatInput.playerReply(event.getPlayer(), msg);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (!ChatInput.contains(event.getPlayer())) return;
        ChatInput.remove(event.getPlayer().getUniqueId());
    }
}

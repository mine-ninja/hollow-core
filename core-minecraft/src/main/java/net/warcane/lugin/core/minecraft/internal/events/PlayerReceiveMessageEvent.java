package net.warcane.lugin.core.minecraft.internal.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rok, Pedro Lucas nmm. Created on 31/08/2025
 * @project lugin-core
 */
@Getter
public class PlayerReceiveMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final Component message;
    private final String rawMessage;
    private final String key;
    private boolean cancelled = false;

    public PlayerReceiveMessageEvent(Player player, Component message, String rawMessage, String key) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.message = message;
        this.rawMessage = rawMessage;
        this.key = key;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}

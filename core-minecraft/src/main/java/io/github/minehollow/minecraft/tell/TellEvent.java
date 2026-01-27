package io.github.minehollow.minecraft.tell;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class TellEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Setter
    private boolean cancelled;

    private final Player player;
    private final UUID targetUUID;
    @Setter
    private String message;

    public TellEvent(Player player, UUID targetUUID, String message) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.message = message;
        this.targetUUID = targetUUID;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}

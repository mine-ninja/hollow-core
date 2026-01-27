package io.github.minehollow.minecraft.event.connection;

import io.github.minehollow.sdk.network.packet.impl.connection.ConnectionHandshakePacket;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectionRequestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final UUID userId;
    private final Side side;
    private final ConnectionHandshakePacket packet;

    private String fallbackMessage;
    private boolean cancelled;
    
    public ConnectionRequestEvent(UUID userId, Side side, ConnectionHandshakePacket packet) {
        super(true);
        this.userId = userId;
        this.side = side;
        this.packet = packet;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public enum Side {
        CURRENT,
        REMOTE
    }
}

package net.warcane.lugin.core.minecraft.centralcart.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class OrderExpiredEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String userId;
    private final String orderId;
    
    public OrderExpiredEvent(String userId, String orderId) {
        this.userId = userId;
        this.orderId = orderId;
    }
    
    public static HandlerList getHandlerList() {
        return handlerList;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}

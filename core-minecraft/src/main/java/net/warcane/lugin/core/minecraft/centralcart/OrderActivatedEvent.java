package net.warcane.lugin.core.minecraft.centralcart;

import net.warcane.lugin.core.minecraft.centralcart.models.Order;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class OrderActivatedEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String userId;
    private final Order order;
    
    public OrderActivatedEvent(String userId, Order order) {
        this.userId = userId;
        this.order = order;
    }
    
    public static HandlerList getHandlerList() {
        return handlerList;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}

package io.github.minehollow.minecraft.gamerule.event;

import io.github.minehollow.minecraft.gamerule.CustomGameRule;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a custom game rule is changed.
 */
@Getter
public class CustomGameRuleChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final World world;
    private final CustomGameRule<?> gameRule;
    private final Object oldValue;
    private final Object newValue;
    private final CommandSender sender;
    private boolean cancelled;
    
    public CustomGameRuleChangeEvent(@NotNull World world, @NotNull CustomGameRule<?> gameRule, @Nullable Object oldValue, @NotNull Object newValue, @Nullable CommandSender sender) {
        this.world = world;
        this.gameRule = gameRule;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.sender = sender;
        this.cancelled = false;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

package io.github.minehollow.zones.event;

import io.github.minehollow.zones.model.Zone;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ZoneEnterEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Zone zone;
    private boolean cancelled;

    public ZoneEnterEvent(@NotNull Player player, @NotNull Zone zone) {
        this.player = player;
        this.zone = zone;
    }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}


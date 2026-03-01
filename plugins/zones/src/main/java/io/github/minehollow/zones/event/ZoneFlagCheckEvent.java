package io.github.minehollow.zones.event;

import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired during every flag resolution. External plugins can override the result.
 */
@Getter
public class ZoneFlagCheckEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location location;
    private final ZoneFlag flag;
    @Setter
    private ZoneFlagState result;

    public ZoneFlagCheckEvent(@NotNull Location location, @NotNull ZoneFlag flag, @NotNull ZoneFlagState result) {
        this.location = location;
        this.flag = flag;
        this.result = result;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}


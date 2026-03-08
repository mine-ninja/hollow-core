package io.github.minehollow.zones;

import io.github.minehollow.zones.event.ZoneFlagCheckEvent;
import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the effective flag state at a location using priority stacking. Fires {@link ZoneFlagCheckEvent} to allow external overrides.
 * <p>
 * All methods are zero-allocation — delegates to {@link ZoneManager#resolveFlag}.
 */
public class ZoneQuery {

    private final ZoneManager manager;

    public ZoneQuery(@NotNull ZoneManager manager) {
        this.manager = manager;
    }

    /**
     * Resolves the effective flag state at a location. Walks the zone stack (highest priority first) until a non-NONE state is found. If no zone defines the
     * flag, returns ALLOW (default open-world behavior).
     */
    @NotNull
    public ZoneFlagState resolve(@NotNull Location loc, @NotNull ZoneFlag flag) {
        return resolve(loc, flag, null);
    }

    /**
     * Resolves the effective flag state, considering a player UUID for member bypass.
     */
    @NotNull
    public ZoneFlagState resolve(@NotNull Location loc, @NotNull ZoneFlag flag, @Nullable UUID playerUuid) {
        return manager.resolveFlag(loc, flag, playerUuid);
    }

    /**
     * Quick check: is the flag denied at this location for this player?
     */
    public boolean isDenied(@NotNull Location loc, @NotNull ZoneFlag flag, @Nullable UUID playerUuid) {
        return resolve(loc, flag, playerUuid) == ZoneFlagState.DENY;
    }

    /**
     * Quick check using raw world + chunk coordinates. Zero allocation.
     */
    public boolean isDenied(@NotNull World world, int x, int y, int z, @NotNull ZoneFlag flag, @Nullable UUID playerUuid) {
        ZoneFlagState resolved = manager.resolveFlag(world.getName(), x, y, z, flag, playerUuid);
        return resolved == ZoneFlagState.DENY;
    }
}


package io.github.minehollow.zones;

import io.github.minehollow.zones.event.ZoneFlagCheckEvent;
import io.github.minehollow.zones.model.Zone;
import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Resolves the effective flag state at a location using priority stacking.
 * Fires {@link ZoneFlagCheckEvent} to allow external overrides.
 */
public class ZoneQuery {

    private final ZoneManager manager;

    public ZoneQuery(@NotNull ZoneManager manager) {
        this.manager = manager;
    }

    /**
     * Resolves the effective flag state at a location.
     * Walks the zone stack (highest priority first) until a non-NONE state is found.
     * If no zone defines the flag, returns ALLOW (default open-world behavior).
     *
     * @param loc  the location to check
     * @param flag the flag to resolve
     * @return the resolved state
     */
    @NotNull
    public ZoneFlagState resolve(@NotNull Location loc, @NotNull ZoneFlag flag) {
        return resolve(loc, flag, null);
    }

    /**
     * Resolves the effective flag state, considering a player UUID for member bypass.
     */
    @NotNull
    public ZoneFlagState resolve(@NotNull Location loc, @NotNull ZoneFlag flag, UUID playerUuid) {
        List<Zone> stack = manager.getZonesAt(loc);
        if (stack.isEmpty()) return ZoneFlagState.ALLOW;

        ZoneFlagState resolved = ZoneFlagState.NONE;
        for (Zone zone : stack) {
            // Members bypass deny flags
            if (playerUuid != null && zone.isMember(playerUuid)) continue;

            ZoneFlagState state = zone.getFlagState(flag);
            if (state != ZoneFlagState.NONE) {
                resolved = state;
                break;
            }
        }

        if (resolved == ZoneFlagState.NONE) resolved = ZoneFlagState.ALLOW;

        // Fire event to allow external overrides
        var event = new ZoneFlagCheckEvent(loc, flag, resolved);
        Bukkit.getPluginManager().callEvent(event);
        return event.getResult();
    }

    /**
     * Quick check: is the flag denied at this location for this player?
     */
    public boolean isDenied(@NotNull Location loc, @NotNull ZoneFlag flag, UUID playerUuid) {
        return resolve(loc, flag, playerUuid) == ZoneFlagState.DENY;
    }
}


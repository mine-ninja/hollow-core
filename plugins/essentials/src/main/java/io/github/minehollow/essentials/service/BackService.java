package io.github.minehollow.essentials.service;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores last "return" locations used by /back.
 */
public class BackService {

    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    private final Set<UUID> ignoreNextTeleportCapture = ConcurrentHashMap.newKeySet();

    public void saveTeleportOrigin(@NotNull UUID playerId, @NotNull Location from) {
        if (ignoreNextTeleportCapture.remove(playerId)) {
            return;
        }
        if (from.getWorld() == null) {
            return;
        }
        backLocations.put(playerId, from.clone());
    }

    public void saveDeathLocation(@NotNull UUID playerId, @NotNull Location deathLocation) {
        if (deathLocation.getWorld() == null) {
            return;
        }
        backLocations.put(playerId, deathLocation.clone());
    }

    public void ignoreNextTeleportCapture(@NotNull UUID playerId) {
        ignoreNextTeleportCapture.add(playerId);
    }

    public @Nullable Location getBackLocation(@NotNull UUID playerId) {
        Location location = backLocations.get(playerId);
        return location == null ? null : location.clone();
    }

    public void evict(@NotNull UUID playerId) {
        backLocations.remove(playerId);
        ignoreNextTeleportCapture.remove(playerId);
    }
}


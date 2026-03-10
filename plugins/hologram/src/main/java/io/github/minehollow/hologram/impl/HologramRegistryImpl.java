package io.github.minehollow.hologram.impl;

import io.github.minehollow.hologram.Hologram;
import io.github.minehollow.hologram.api.HologramRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central hologram registry - manages lifecycle, persistence, and lookups.
 */
public class HologramRegistryImpl implements HologramRegistry {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final HologramStorage storage;
    private final double renderDistance;

    public HologramRegistryImpl(@NotNull HologramStorage storage, double renderDistance) {
        this.storage = storage;
        this.renderDistance = renderDistance;
    }

    public double getRenderDistance() {
        return renderDistance;
    }

    public double getRenderDistanceSq() {
        return renderDistance * renderDistance;
    }

    public void loadAll() {
        Map<String, Hologram> loaded = storage.loadAll();
        for (var entry : loaded.entrySet()) {
            Hologram hologram = entry.getValue();
            hologram.spawn();
            holograms.put(entry.getKey(), hologram);
        }
    }

    public void unloadAll() {
        saveAll();
        for (Hologram hologram : holograms.values()) {
            hologram.despawn();
        }
        holograms.clear();
    }

    public void saveAll() {
        Map<String, Hologram> persistent = new ConcurrentHashMap<>();
        for (var entry : holograms.entrySet()) {
            if (entry.getValue().isPersistent()) {
                persistent.put(entry.getKey(), entry.getValue());
            }
        }
        storage.saveAsync(persistent);
    }

    public void saveIfDirty() {
        Map<String, Hologram> persistent = new ConcurrentHashMap<>();
        for (var entry : holograms.entrySet()) {
            if (entry.getValue().isPersistent()) {
                persistent.put(entry.getKey(), entry.getValue());
            }
        }
        storage.saveIfDirty(persistent);
    }

    public void markDirty() {
        storage.markDirty();
        saveAll();
    }

    @Override
    public @NotNull Hologram create(@NotNull String id, @NotNull Location location) {
        if (holograms.containsKey(id)) {
            throw new IllegalArgumentException("Hologram with id '" + id + "' already exists.");
        }

        Hologram hologram = new Hologram(id, location, 0.3, true);
        hologram.spawn();
        holograms.put(id, hologram);
        markDirty();
        return hologram;
    }

    @Override
    public @NotNull Hologram createTemporary(@NotNull String id, @NotNull Location location) {
        if (holograms.containsKey(id)) {
            throw new IllegalArgumentException("Hologram with id '" + id + "' already exists.");
        }

        Hologram hologram = new Hologram(id, location, 0.3, false);
        hologram.spawn();
        holograms.put(id, hologram);
        return hologram;
    }

    @Override
    public @Nullable Hologram get(@NotNull String id) {
        return holograms.get(id);
    }

    @Override
    public @NotNull Collection<Hologram> getAll() {
        return holograms.values();
    }

    @Override
    public boolean remove(@NotNull String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram == null) {
            return false;
        }

        hologram.despawn();
        markDirty();
        return true;
    }
}

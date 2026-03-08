package io.github.minehollow.npc.impl;

import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcRegistry;
import io.github.minehollow.npc.config.NpcConfig;
import io.github.minehollow.npc.config.NpcStorage;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central NPC registry — manages lifecycle, config, and entity-ID-based lookups.
 */
public class NpcRegistryImpl implements NpcRegistry {

    private final Map<String, NpcImpl> npcs = new ConcurrentHashMap<>();
    private final Map<Integer, NpcImpl> entityIdIndex = new ConcurrentHashMap<>();
    private final NpcStorage storage;
    private final double renderDistance;

    public NpcRegistryImpl(@NotNull NpcStorage storage, double renderDistance) {
        this.storage = storage;
        this.renderDistance = renderDistance;
    }

    public void tickAll(){
        for (NpcImpl npc : npcs.values()) {
            npc.onTick();
        }
    }

    /**
     * Loads all NPCs from storage and spawns them.
     */
    public void loadAll() {
        Map<String, NpcConfig> configs = storage.loadAll();
        for (var entry : configs.entrySet()) {
            NpcImpl npc = new NpcImpl(entry.getValue(), renderDistance);
            npc.spawn();
            npcs.put(entry.getKey(), npc);
            entityIdIndex.put(npc.getEntityId(), npc);
        }
    }

    /**
     * Despawns all NPCs and saves to storage.
     */
    public void unloadAll() {
        saveAll();
        for (NpcImpl npc : npcs.values()) {
            npc.despawn();
        }
        npcs.clear();
        entityIdIndex.clear();
    }

    public void saveAll() {
        Map<String, NpcConfig> configs = new ConcurrentHashMap<>();
        for (var entry : npcs.entrySet()) {
            configs.put(entry.getKey(), entry.getValue().getConfig());
        }
        storage.saveAsync(configs);
    }

    public void saveIfDirty() {
        Map<String, NpcConfig> configs = new ConcurrentHashMap<>();
        for (var entry : npcs.entrySet()) {
            configs.put(entry.getKey(), entry.getValue().getConfig());
        }
        storage.saveIfDirty(configs);
    }

    // ── NpcRegistry interface ────────────────────────────────

    @Override
    public @NotNull Npc create(@NotNull String id, @NotNull Location location) {
        if (npcs.containsKey(id)) {
            throw new IllegalArgumentException("NPC with id '" + id + "' already exists.");
        }

        NpcConfig config = new NpcConfig(id, location);
        NpcImpl npc = new NpcImpl(config, renderDistance);
        npc.spawn();

        npcs.put(id, npc);
        entityIdIndex.put(npc.getEntityId(), npc);
        markDirty();
        return npc;
    }

    @Override
    public @Nullable Npc get(@NotNull String id) {
        return npcs.get(id);
    }

    @Override
    public @NotNull Collection<Npc> getAll() {
        return Collection.class.cast(npcs.values());
    }

    @Override
    public boolean remove(@NotNull String id) {
        NpcImpl npc = npcs.remove(id);
        if (npc == null) return false;

        entityIdIndex.remove(npc.getEntityId());
        npc.despawn();
        markDirty();
        return true;
    }

    @Override
    public @Nullable Npc getByEntityId(int entityId) {
        return entityIdIndex.get(entityId);
    }

    /**
     * Returns all NpcImpl instances (internal use for visibility checks).
     */
    public @NotNull Collection<NpcImpl> getAllImpl() {
        return npcs.values();
    }

    /**
     * Marks as dirty and triggers an immediate async save so changes
     * are persisted right away (e.g. after command mutations).
     */
    public void markDirty() {
        storage.markDirty();
        saveAll();
    }
}


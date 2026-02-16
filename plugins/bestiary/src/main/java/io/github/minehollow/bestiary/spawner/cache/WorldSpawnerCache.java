package io.github.minehollow.bestiary.spawner.cache;

import io.github.minehollow.bestiary.spawner.CustomMobSpawner;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class WorldSpawnerCache {

    /**
     * Usamos ConcurrentHashMap apenas para separar os mundos.
     * O valor interno é um mapa IMUTÁVEL. Toda escrita gera uma nova cópia (Copy-On-Write).
     * Isso permite leitura simultânea por infinitas threads sem NENHUM lock.
     */
    private final Map<String, Long2ObjectMap<UUID[]>> worldSpawners = new ConcurrentHashMap<>();

    public void forEachSpawnersInChunk(
        @NotNull String worldName,
        int chunkX,
        int chunkZ,
        @NotNull Function<UUID, CustomMobSpawner> spawnerResolver,
        @NotNull Consumer<CustomMobSpawner> consumer,
        @Nullable Predicate<CustomMobSpawner> filter
    ) {
        final var chunkMap = worldSpawners.get(worldName);
        if (chunkMap == null) return;

        final UUID[] spawnerIds = chunkMap.get(Chunk.getChunkKey(chunkX, chunkZ));
        if (spawnerIds == null) return;

        for (UUID id : spawnerIds) {
            CustomMobSpawner spawner = spawnerResolver.apply(id);
            if (spawner != null && (filter == null || filter.test(spawner))) {
                consumer.accept(spawner);
            }
        }
    }

    public void unregisterSpawner(@NotNull UUID spawnerId) {
        worldSpawners.forEach((worldName, chunkMap) -> {
            chunkMap.forEach((chunkKey, spawnerIds) -> {
                if (Arrays.asList(spawnerIds).contains(spawnerId)) {
                    UUID[] newArray = Arrays.stream(spawnerIds)
                        .filter(id -> !id.equals(spawnerId))
                        .toArray(UUID[]::new);

                    Long2ObjectMap<UUID[]> newChunkMap = new Long2ObjectOpenHashMap<>(chunkMap);
                    newChunkMap.put(chunkKey, newArray);
                    worldSpawners.put(worldName, newChunkMap);
                }
            });
        });
    }

    public void registerSpawner(@NotNull CustomMobSpawner spawner) {
        this.registerSpawner(
            spawner.getWorld().getName(),
            spawner.getChunkX(),
            spawner.getChunkZ(),
            spawner.getUniqueId()
        );
    }

    public void registerSpawner(String worldName, int chunkX, int chunkZ, UUID spawnerId) {
        long key = Chunk.getChunkKey(chunkX, chunkZ);

        worldSpawners.compute(worldName, (name, oldMap) -> {
            Long2ObjectMap<UUID[]> newMap = (oldMap == null)
                ? new Long2ObjectOpenHashMap<>()
                : new Long2ObjectOpenHashMap<>(oldMap);

            UUID[] oldArray = newMap.get(key);
            UUID[] newArray;

            if (oldArray == null) {
                newArray = new UUID[]{spawnerId};
            } else {
                newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
                newArray[oldArray.length] = spawnerId;
            }

            newMap.put(key, newArray);
            return newMap;
        });
    }

    public List<UUID> getSpawnersInChunk(@NotNull String worldName, int chunkX, int chunkZ) {
        var chunkMap = worldSpawners.get(worldName);
        if (chunkMap == null) return Collections.emptyList();

        UUID[] array = chunkMap.get(Chunk.getChunkKey(chunkX, chunkZ));
        return array == null ? Collections.emptyList() : Arrays.asList(array);
    }
}

package io.github.minehollow.bestiary.spawner;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.spawner.cache.WorldSpawnerCache;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.util.data.MongoRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class SpawnerService {

    private final BestiaryPlugin plugin;

    private final Map<UUID, CustomMobSpawner> cachedSpawners = new ConcurrentHashMap<>();
    private final WorldSpawnerCache worldSpawnerCache = new WorldSpawnerCache();

    // O(1) lookup map
    private final MongoRepository<UUID, CustomMobSpawner> repository = new MongoRepository<>(CustomMobSpawner.class);

    public SpawnerService(BestiaryPlugin plugin) {
        this.plugin = plugin;
    }

    public void preCacheAllSpawners() {
        Tasks.runAsync(() -> {
            final var spawners = repository.queryAll();
            spawners.forEach(this::cacheSpawner);

            Bukkit.getConsoleSender().sendMessage("§a[Bestiary] §7Carregados §a" + spawners.size() + " §7spawners personalizados.");
        });
    }

    public void fetchSpawner(@NotNull UUID spawnerId, @NotNull BiConsumer<CustomMobSpawner, Throwable> callback) {
        Tasks.runAsync(() -> {
            try {
                var spawner = cachedSpawners.get(spawnerId);
                if (spawner == null) {
                    spawner = repository.findById(spawnerId);
                    if (spawner != null) {
                        this.cacheSpawner(spawner);
                    }
                }

                callback.accept(spawner, null);
            } catch (Throwable t) {
                callback.accept(null, t);
            }
        });
    }


    public void saveSpawner(@NotNull CustomMobSpawner spawner) {
        Tasks.runAsync(() -> {
            repository.save(spawner, CustomMobSpawner::getUniqueId);
            this.cacheSpawner(spawner);
        });
    }

    public void deleteSpawner(@NotNull UUID spawnerId) {
        Tasks.runAsync(() -> {
            repository.deleteById(spawnerId);
            cachedSpawners.remove(spawnerId);
            worldSpawnerCache.unregisterSpawner(spawnerId);
        });
    }

    public void cacheSpawner(@NotNull CustomMobSpawner spawner) {
        cachedSpawners.put(spawner.getUniqueId(), spawner);
        worldSpawnerCache.registerSpawner(spawner);
    }
}

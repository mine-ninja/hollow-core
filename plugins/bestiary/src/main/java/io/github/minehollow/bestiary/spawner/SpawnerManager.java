package io.github.minehollow.bestiary.spawner;

import io.github.minehollow.bestiary.monster.ActiveMonster;
import io.github.minehollow.bestiary.monster.MonsterManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class SpawnerManager implements Listener {

    private final MonsterManager monsterManager;

    private final Map<UUID, MonsterSpawner> spawners = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> aliveEntities = new HashMap<>();

    private final Long2ObjectMap<List<MonsterSpawner>> chunkIndex = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<Boolean> loadedChunks = new Long2ObjectOpenHashMap<>();

    public SpawnerManager(@NotNull Plugin plugin, @NotNull MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 20L);
    }

    public void addSpawner(@NotNull MonsterSpawner spawner) {
        spawners.put(spawner.getUniqueId(), spawner);
        aliveEntities.put(spawner.getUniqueId(), new HashSet<>());

        long key = chunkKey(spawner.getLocation());
        chunkIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(spawner);
    }

    public void removeSpawner(@NotNull UUID id) {
        MonsterSpawner spawner = spawners.remove(id);
        if (spawner == null) {
            return;
        }
        aliveEntities.remove(id);

        long key = chunkKey(spawner.getLocation());
        List<MonsterSpawner> list = chunkIndex.get(key);
        if (list != null) {
            list.remove(spawner);
        }
    }

    public void onMonsterDeath(@NotNull UUID spawnerUUID, @NotNull UUID entityUUID) {
        Set<UUID> alive = aliveEntities.get(spawnerUUID);
        if (alive != null) {
            alive.remove(entityUUID);
        }
    }


    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        loadedChunks.put(chunkKey(event.getChunk()), Boolean.TRUE);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        long key = chunkKey(event.getChunk());
        loadedChunks.remove(key);

        // Reseta timers dos spawners no chunk descarregado
        List<MonsterSpawner> list = chunkIndex.get(key);
        if (list != null) {
            list.forEach(MonsterSpawner::resetTimer);
        }
    }

    public Collection<MonsterSpawner> getAll() {
        return spawners.values();
    }

    private void tick() {
        for (MonsterSpawner spawner : spawners.values()) {
            if (!loadedChunks.containsKey(chunkKey(spawner.getLocation()))) {
                continue;
            }
            if (!hasPlayerNearby(spawner.getLocation(), spawner.getActivationRadius())) {
                spawner.resetTimer();
                continue;
            }
            if (spawner.pollSpawnReady()) {
                doSpawn(spawner);
            }
        }
    }

    private void doSpawn(@NotNull MonsterSpawner spawner) {
        Set<UUID> alive = aliveEntities.computeIfAbsent(spawner.getUniqueId(), k -> new HashSet<>());
        alive.removeIf(uuid -> monsterManager.getActive(uuid) == null);

        int slots = spawner.getMaxAlive() == 0
                    ? spawner.getSpawnCount()
                    : Math.min(spawner.getSpawnCount(), spawner.getMaxAlive() - alive.size());

        for (int i = 0; i < slots; i++) {
            ActiveMonster monster = monsterManager.spawn(
                spawner.getMonsterModelId(),
                randomLocationAround(spawner.getLocation(), spawner.getActivationRadius())
            );
            if (monster != null) {
                alive.add(monster.entity().getUniqueId());
            }
        }
    }

    private static boolean hasPlayerNearby(@NotNull Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }
        double radiusSq = radius * radius;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private static Location randomLocationAround(@NotNull Location center, double radius) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(Math.PI * 2);
        double dist = rng.nextDouble(radius);
        return new Location(
            center.getWorld(),
            center.getX() + Math.cos(angle) * dist,
            center.getY(),
            center.getZ() + Math.sin(angle) * dist
        );
    }

    private static long chunkKey(@NotNull Location loc) {
        return chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private static long chunkKey(@NotNull Chunk chunk) {
        return chunkKey(chunk.getX(), chunk.getZ());
    }

    private static long chunkKey(int cx, int cz) {
        return (long) cx << 32 | (cz & 0xFFFFFFFFL);
    }
}

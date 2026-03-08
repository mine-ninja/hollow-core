package io.github.minehollow.zones;

import io.github.minehollow.zones.model.Zone;
import io.github.minehollow.zones.model.ZoneBounds;
import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import io.github.minehollow.zones.model.ZoneType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages all zones: loading, saving, indexing (chunk map + cuboid sorted set), and spatial queries.
 * <p>
 * Cuboid zones live in a {@link TreeSet} sorted by priority descending, so insert/remove are
 * O(log n) and iteration is always in priority order — no re-sorting ever needed.
 * <p>
 * Chunk zones use a {@link Long2ObjectOpenHashMap} for O(1) primitive-key lookup.
 * <p>
 * All hot-path query methods are zero-allocation.
 */
@Slf4j
public class ZoneManager {

    /** Priority descending, then by id for stable uniqueness inside the TreeSet */
    private static final Comparator<Zone> PRIORITY_DESC = Comparator
        .comparingInt(Zone::getPriority).reversed()
        .thenComparing(Zone::getId);

    private final JavaPlugin plugin;
    private final File configFile;

    /** All zones by id */
    @Getter
    private final Map<String, Zone> zones = new HashMap<>();

    /** Chunk-type zones indexed by packed chunk key */
    private final Long2ObjectOpenHashMap<ArrayList<Zone>> chunkIndex = new Long2ObjectOpenHashMap<>();

    /** Cuboid-type zones, always sorted by priority descending (O(log n) insert/remove) */
    private final TreeSet<Zone> cuboidZones = new TreeSet<>(PRIORITY_DESC);

    public ZoneManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    // ── Loading ────────────────────────────────────────

    public void loadAll() {
        zones.clear();
        chunkIndex.clear();
        cuboidZones.clear();

        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection root = yaml.getConfigurationSection("zones");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                addZone(deserialize(id, sec));
            } catch (Exception e) {
                log.warn("Failed to load zone '{}': {}", id, e.getMessage());
            }
        }

        log.info("Loaded {} zones ({} chunk, {} cuboid)",
            zones.size(),
            zones.values().stream().filter(z -> z.getType() == ZoneType.CHUNK).count(),
            cuboidZones.size());
    }

    private Zone deserialize(@NotNull String id, @NotNull ConfigurationSection sec) {
        ZoneType type = ZoneType.valueOf(sec.getString("type", "CUBOID").toUpperCase(Locale.ROOT));
        String world = sec.getString("world", "world");
        String displayName = sec.getString("display-name", id);
        int priority = sec.getInt("priority", 0);

        List<Integer> b = sec.getIntegerList("bounds");
        if (b.size() != 6) throw new IllegalArgumentException("bounds must have 6 integers");
        ZoneBounds bounds = new ZoneBounds(b.get(0), b.get(1), b.get(2), b.get(3), b.get(4), b.get(5));

        Map<ZoneFlag, ZoneFlagState> flags = new EnumMap<>(ZoneFlag.class);
        ConfigurationSection flagSec = sec.getConfigurationSection("flags");
        if (flagSec != null) {
            for (String key : flagSec.getKeys(false)) {
                ZoneFlag flag = ZoneFlag.fromKey(key);
                ZoneFlagState state = ZoneFlagState.fromString(flagSec.getString(key, "none"));
                if (flag != null && state != null) flags.put(flag, state);
            }
        }

        ObjectOpenHashSet<UUID> members = new ObjectOpenHashSet<>();
        for (String s : sec.getStringList("members")) {
            try { members.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }

        return new Zone(id, type, world, displayName, priority, bounds, flags, members);
    }

    // ── Saving ─────────────────────────────────────────

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Zone z : zones.values()) {
            String path = "zones." + z.getId();
            yaml.set(path + ".type", z.getType().name());
            yaml.set(path + ".display-name", z.getDisplayName());
            yaml.set(path + ".world", z.getWorld());
            yaml.set(path + ".priority", z.getPriority());
            yaml.set(path + ".bounds", List.of(
                z.getBounds().minX(), z.getBounds().minY(), z.getBounds().minZ(),
                z.getBounds().maxX(), z.getBounds().maxY(), z.getBounds().maxZ()
            ));
            for (Map.Entry<ZoneFlag, ZoneFlagState> entry : z.getFlags().entrySet()) {
                yaml.set(path + ".flags." + entry.getKey().configKey(), entry.getValue().name().toLowerCase(Locale.ROOT));
            }
            yaml.set(path + ".members", z.getMembers().stream().map(UUID::toString).toList());
        }

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            log.error("Failed to save zones config", e);
        }
    }

    // ── Index management ───────────────────────────────

    public void addZone(@NotNull Zone zone) {
        zones.put(zone.getId(), zone);
        if (zone.getType() == ZoneType.CHUNK) {
            indexChunkZone(zone);
        } else {
            cuboidZones.add(zone); // O(log n), always sorted
        }
    }

    public void removeZone(@NotNull String id) {
        Zone zone = zones.remove(id);
        if (zone == null) return;
        if (zone.getType() == ZoneType.CHUNK) {
            chunkIndex.values().forEach(list -> list.remove(zone));
        } else {
            cuboidZones.remove(zone); // O(log n)
        }
    }

    private void indexChunkZone(@NotNull Zone zone) {
        ZoneBounds b = zone.getBounds();
        int minCX = b.minX() >> 4, maxCX = b.maxX() >> 4;
        int minCZ = b.minZ() >> 4, maxCZ = b.maxZ() >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                chunkIndex.computeIfAbsent(chunkKey(cx, cz), k -> new ArrayList<>(2)).add(zone);
            }
        }
    }

    // ── Zero-allocation query API ──────────────────────

    /**
     * Iterates over every zone that contains the given location.
     * Visits chunk-indexed zones first, then cuboid zones (priority descending).
     * Zero allocation — no list is created.
     */
    public void forEachZoneAt(@NotNull Location loc, @NotNull Consumer<Zone> consumer) {
        if (loc.getWorld() == null) return;
        forEachZoneAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), consumer);
    }

    /**
     * Iterates over every zone that contains the given block coordinates.
     * Zero allocation — no list, no Location object needed.
     */
    public void forEachZoneAt(@NotNull String world, int x, int y, int z, @NotNull Consumer<Zone> consumer) {
        long key = chunkKey(x >> 4, z >> 4);
        ArrayList<Zone> chunk = chunkIndex.get(key);
        if (chunk != null) {
            for (int i = 0, len = chunk.size(); i < len; i++) {
                Zone zone = chunk.get(i);
                if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                    consumer.accept(zone);
                }
            }
        }

        // TreeSet iterates in priority-descending order naturally
        for (Zone zone : cuboidZones) {
            if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                consumer.accept(zone);
            }
        }
    }

    /**
     * Tests if any zone at the location matches the predicate.
     * Short-circuits on first match. Zero allocation.
     */
    public boolean anyZoneAt(@NotNull Location loc, @NotNull Predicate<Zone> predicate) {
        if (loc.getWorld() == null) return false;
        String world = loc.getWorld().getName();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();

        long key = chunkKey(x >> 4, z >> 4);
        ArrayList<Zone> chunk = chunkIndex.get(key);
        if (chunk != null) {
            for (int i = 0, len = chunk.size(); i < len; i++) {
                Zone zone = chunk.get(i);
                if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z) && predicate.test(zone)) {
                    return true;
                }
            }
        }

        for (Zone zone : cuboidZones) {
            if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z) && predicate.test(zone)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if any zone in the given chunk column matches the predicate.
     * Zero allocation — no Location needed.
     */
    public boolean anyZoneInChunk(@NotNull String world, int chunkX, int chunkZ, @NotNull Predicate<Zone> predicate) {
        long key = chunkKey(chunkX, chunkZ);
        ArrayList<Zone> chunk = chunkIndex.get(key);
        if (chunk != null) {
            for (int i = 0, len = chunk.size(); i < len; i++) {
                Zone zone = chunk.get(i);
                if (zone.getWorld().equals(world) && predicate.test(zone)) {
                    return true;
                }
            }
        }

        for (Zone zone : cuboidZones) {
            if (zone.getWorld().equals(world) && zone.getBounds().intersectsChunk(chunkX, chunkZ) && predicate.test(zone)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a flag state at a location by walking zones in priority order.
     * Returns the first non-NONE state found, or ALLOW if no zone defines the flag.
     * Zero allocation.
     *
     * @param playerUuid if non-null, zones where the player is a member are skipped
     */
    @NotNull
    public ZoneFlagState resolveFlag(@NotNull Location loc, @NotNull ZoneFlag flag, @Nullable UUID playerUuid) {
        if (loc.getWorld() == null) return ZoneFlagState.ALLOW;
        return resolveFlag(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), flag, playerUuid);
    }

    /**
     * Resolves a flag state at raw coordinates. Zero allocation, no Location needed.
     */
    @NotNull
    public ZoneFlagState resolveFlag(@NotNull String world, int x, int y, int z,
                                     @NotNull ZoneFlag flag, @Nullable UUID playerUuid) {
        int bestPriority = Integer.MIN_VALUE;
        ZoneFlagState bestState = ZoneFlagState.NONE;

        // Chunk zones (unordered, scan all)
        long key = chunkKey(x >> 4, z >> 4);
        ArrayList<Zone> chunk = chunkIndex.get(key);
        if (chunk != null) {
            for (int i = 0, len = chunk.size(); i < len; i++) {
                Zone zone = chunk.get(i);
                if (!zone.getWorld().equals(world) || !zone.getBounds().contains(x, y, z)) continue;
                if (playerUuid != null && zone.isMember(playerUuid)) continue;
                ZoneFlagState state = zone.getFlagState(flag);
                if (state != ZoneFlagState.NONE && zone.getPriority() > bestPriority) {
                    bestPriority = zone.getPriority();
                    bestState = state;
                }
            }
        }

        // Cuboid zones (TreeSet, priority descending — early exit once priority can't beat best)
        for (Zone zone : cuboidZones) {
            if (zone.getPriority() <= bestPriority) break;
            if (!zone.getWorld().equals(world) || !zone.getBounds().contains(x, y, z)) continue;
            if (playerUuid != null && zone.isMember(playerUuid)) continue;
            ZoneFlagState state = zone.getFlagState(flag);
            if (state != ZoneFlagState.NONE) {
                bestState = state;
                break; // highest priority cuboid that matches — done
            }
        }

        return bestState == ZoneFlagState.NONE ? ZoneFlagState.ALLOW : bestState;
    }

    /**
     * Returns the highest-priority zone containing the location, or null. Zero allocation.
     */
    @Nullable
    public Zone getHighestPriorityZone(@NotNull Location loc) {
        if (loc.getWorld() == null) return null;
        String world = loc.getWorld().getName();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();

        Zone best = null;

        // Chunk zones (unordered, find highest)
        long key = chunkKey(x >> 4, z >> 4);
        ArrayList<Zone> chunk = chunkIndex.get(key);
        if (chunk != null) {
            for (int i = 0, len = chunk.size(); i < len; i++) {
                Zone zone = chunk.get(i);
                if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                    if (best == null || zone.getPriority() > best.getPriority()) {
                        best = zone;
                    }
                }
            }
        }

        // Cuboid zones (TreeSet, priority descending — first match wins or early exit)
        for (Zone zone : cuboidZones) {
            if (best != null && zone.getPriority() <= best.getPriority()) break;
            if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                best = zone;
                break;
            }
        }

        return best;
    }

    @Nullable
    public Zone getZone(@NotNull String id) {
        return zones.get(id);
    }

    public void reload() {
        loadAll();
    }

    // ── Util ───────────────────────────────────────────

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}


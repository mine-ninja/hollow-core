package io.github.minehollow.zones;

import io.github.minehollow.zones.model.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages all zones: loading, saving, indexing (chunk map + cuboid list),
 * and spatial queries.
 */
@Slf4j
public class ZoneManager {

    private final JavaPlugin plugin;
    private final File configFile;

    /** All zones by id */
    @Getter
    private final Object2ObjectOpenHashMap<String, Zone> zones = new Object2ObjectOpenHashMap<>();

    /** Chunk-type zones indexed by packed chunk key */
    private final Long2ObjectOpenHashMap<ObjectArrayList<Zone>> chunkIndex = new Long2ObjectOpenHashMap<>();

    /** Cuboid-type zones sorted by priority descending for fast scan */
    private final ObjectArrayList<Zone> cuboidZones = new ObjectArrayList<>();

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
                Zone zone = deserialize(id, sec);
                addZone(zone);
            } catch (Exception e) {
                log.warn("Failed to load zone '{}': {}", id, e.getMessage());
            }
        }

        cuboidZones.sort(Comparator.comparingInt(Zone::getPriority).reversed());
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

            List<String> memberStrings = z.getMembers().stream().map(UUID::toString).toList();
            yaml.set(path + ".members", memberStrings);
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
            cuboidZones.add(zone);
            cuboidZones.sort(Comparator.comparingInt(Zone::getPriority).reversed());
        }
    }

    public void removeZone(@NotNull String id) {
        Zone zone = zones.remove(id);
        if (zone == null) return;
        if (zone.getType() == ZoneType.CHUNK) {
            chunkIndex.values().forEach(list -> list.remove(zone));
        } else {
            cuboidZones.remove(zone);
        }
    }

    private void indexChunkZone(@NotNull Zone zone) {
        ZoneBounds b = zone.getBounds();
        int minCX = b.minX() >> 4;
        int maxCX = b.maxX() >> 4;
        int minCZ = b.minZ() >> 4;
        int maxCZ = b.maxZ() >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = chunkKey(cx, cz);
                chunkIndex.computeIfAbsent(key, k -> new ObjectArrayList<>(2)).add(zone);
            }
        }
    }

    // ── Querying ───────────────────────────────────────

    /**
     * Returns all zones that contain the given location, sorted by priority descending.
     */
    @NotNull
    public List<Zone> getZonesAt(@NotNull Location loc) {
        if (loc.getWorld() == null) return Collections.emptyList();
        String world = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        ObjectArrayList<Zone> result = new ObjectArrayList<>(4);

        // O(1) chunk lookup
        long key = chunkKey(x >> 4, z >> 4);
        ObjectArrayList<Zone> chunkZones = chunkIndex.get(key);
        if (chunkZones != null) {
            for (int i = 0, len = chunkZones.size(); i < len; i++) {
                Zone zone = chunkZones.get(i);
                if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                    result.add(zone);
                }
            }
        }

        // O(n) cuboid scan (sorted by priority)
        for (int i = 0, len = cuboidZones.size(); i < len; i++) {
            Zone zone = cuboidZones.get(i);
            if (zone.getWorld().equals(world) && zone.getBounds().contains(x, y, z)) {
                result.add(zone);
            }
        }

        result.sort(Comparator.comparingInt(Zone::getPriority).reversed());
        return result;
    }

    @Nullable
    public Zone getHighestPriorityZone(@NotNull Location loc) {
        List<Zone> stack = getZonesAt(loc);
        return stack.isEmpty() ? null : stack.getFirst();
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


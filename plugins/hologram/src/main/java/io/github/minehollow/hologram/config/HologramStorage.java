package io.github.minehollow.hologram.config;

import io.github.minehollow.hologram.Hologram;
import io.github.minehollow.hologram.line.*;
import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Loads/saves hologram data to {@code holograms.yml}. Saves are performed
 * asynchronously with a dirty flag to avoid redundant writes.
 */
public class HologramStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public HologramStorage(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
    }

    // ── Load (synchronous — called at startup) ───────────────

    public @NotNull Map<String, Hologram> loadAll() {
        if (!file.exists()) return new ConcurrentHashMap<>();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("holograms");
        if (root == null) return new ConcurrentHashMap<>();

        Map<String, Hologram> result = new ConcurrentHashMap<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            Hologram holo = deserializeHologram(id, sec);
            if (holo != null) {
                result.put(id, holo);
            }
        }
        return result;
    }

    // ── Save ─────────────────────────────────────────────────

    public void markDirty() {
        dirty.set(true);
    }

    public void saveIfDirty(@NotNull Map<String, Hologram> holograms) {
        if (!dirty.compareAndSet(true, false)) return;
        saveAll(holograms);
    }

    public void saveAsync(@NotNull Map<String, Hologram> holograms) {
        dirty.set(false);
        Map<String, Hologram> snapshot = new LinkedHashMap<>(holograms);
        Tasks.runAsync(() -> doSave(snapshot));
    }

    public void saveAll(@NotNull Map<String, Hologram> holograms) {
        doSave(new LinkedHashMap<>(holograms));
    }

    private void doSave(@NotNull Map<String, Hologram> holograms) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("holograms");

        for (var entry : holograms.entrySet()) {
            serializeHologram(root.createSection(entry.getKey()), entry.getValue());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Failed to create hologram data folder: " + parent.getAbsolutePath());
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save holograms.yml", e);
        }
    }

    // ── Serialization ────────────────────────────────────────

    private void serializeHologram(@NotNull ConfigurationSection sec, @NotNull Hologram holo) {
        Location loc = holo.getLocation();
        sec.set("location.world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        sec.set("location.x", loc.getX());
        sec.set("location.y", loc.getY());
        sec.set("location.z", loc.getZ());
        sec.set("location.yaw", (double) loc.getYaw());
        sec.set("location.pitch", (double) loc.getPitch());

        sec.set("line-spacing", holo.getLineSpacing());

        List<Map<String, Object>> linesList = new ArrayList<>();
        for (HologramLine line : holo.getLines()) {
            linesList.add(line.serialize());
        }
        sec.set("lines", linesList);
    }

    // ── Deserialization ──────────────────────────────────────

    private Hologram deserializeHologram(@NotNull String id, @NotNull ConfigurationSection sec) {
        ConfigurationSection locSec = sec.getConfigurationSection("location");
        if (locSec == null) return null;

        String worldName = locSec.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Hologram '" + id + "' references unknown world '" + worldName + "', skipping.");
            return null;
        }

        Location loc = new Location(world,
                locSec.getDouble("x"), locSec.getDouble("y"), locSec.getDouble("z"),
                (float) locSec.getDouble("yaw"), (float) locSec.getDouble("pitch"));

        double lineSpacing = sec.getDouble("line-spacing", 0.3);

        Hologram holo = new Hologram(id, loc, lineSpacing, true);

        List<?> linesList = sec.getList("lines");
        if (linesList != null) {
            for (Object obj : linesList) {
                if (obj instanceof Map<?, ?> map) {
                    HologramLine line = deserializeLine(map);
                    if (line != null) {
                        holo.addLine(line);
                    }
                }
            }
        }

        return holo;
    }

    private HologramLine deserializeLine(@NotNull Map<?, ?> map) {
        String type = String.valueOf(map.get("type")).toUpperCase();
        float scale = map.containsKey("scale") ? ((Number) map.get("scale")).floatValue() : 1.0f;

        return switch (type) {
            case "TEXT" -> {
                Object textObj = map.get("text");
                String text = textObj != null ? String.valueOf(textObj) : "";
                int bgColor = map.containsKey("background-color") ? ((Number) map.get("background-color")).intValue() : 0x0;
                boolean shadow = !map.containsKey("shadow") || Boolean.parseBoolean(String.valueOf(map.get("shadow")));
                yield new TextDisplayHologramLine(text, scale, bgColor, shadow);
            }
            case "ITEM" -> {
                Object matObj = map.get("material");
                String materialName = matObj != null ? String.valueOf(matObj) : "STONE";
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.STONE;
                }
                yield new ItemDisplayHologramLine(material, scale);
            }
            case "BLOCK" -> {
                Object matObj = map.get("material");
                String materialName = matObj != null ? String.valueOf(matObj) : "STONE";
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.STONE;
                }
                float blockScale = map.containsKey("scale") ? ((Number) map.get("scale")).floatValue() : 0.5f;
                yield new BlockDisplayHologramLine(material, blockScale);
            }
            default -> {
                plugin.getLogger().warning("Unknown hologram line type: " + type);
                yield null;
            }
        };
    }
}

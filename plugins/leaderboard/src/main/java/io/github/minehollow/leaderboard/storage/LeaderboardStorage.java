package io.github.minehollow.leaderboard.storage;

import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.sdk.stats.StatPeriod;
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
import java.util.logging.Level;

/**
 * Loads and saves {@link LeaderboardConfig} entries from/to {@code leaderboards.yml}.
 */
public class LeaderboardStorage {

    private final JavaPlugin plugin;
    private final File file;

    public LeaderboardStorage(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
        if (!file.exists()) {
            plugin.saveResource("leaderboards.yml", false);
        }
    }

    public @NotNull Map<String, LeaderboardConfig> loadAll() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("leaderboards");
        if (section == null) return new LinkedHashMap<>();

        Map<String, LeaderboardConfig> result = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection lb = section.getConfigurationSection(id);
            if (lb == null) continue;

            LeaderboardConfig config = deserialize(id, lb);
            if (config != null) {
                result.put(id, config);
            }
        }
        return result;
    }

    public void saveAll(@NotNull Map<String, LeaderboardConfig> configs) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("leaderboards");

        for (var entry : configs.entrySet()) {
            serialize(section.createSection(entry.getKey()), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save leaderboards.yml", e);
        }
    }

    // ── Serialization ────────────────────────────────────────

    private void serialize(@NotNull ConfigurationSection sec, @NotNull LeaderboardConfig config) {
        sec.set("stat-key", config.statKey());
        sec.set("period", config.period().name());
        sec.set("display-name", config.displayName());
        sec.set("max-entries", config.maxEntries());
        sec.set("header", config.headerLines());
        sec.set("entry-format", config.entryFormat());
        sec.set("footer", config.footerLines());
        sec.set("empty-entry", config.emptyEntry());
        sec.set("icon", config.icon().name());

        Location loc = config.hologramLoc();
        if (loc != null && loc.getWorld() != null) {
            sec.set("hologram.world", loc.getWorld().getName());
            sec.set("hologram.x", loc.getX());
            sec.set("hologram.y", loc.getY());
            sec.set("hologram.z", loc.getZ());
            sec.set("hologram.yaw", (double) loc.getYaw());
            sec.set("hologram.pitch", (double) loc.getPitch());
        }
    }

    private LeaderboardConfig deserialize(@NotNull String id, @NotNull ConfigurationSection sec) {
        String statKey = sec.getString("stat-key");
        if (statKey == null) {
            plugin.getLogger().warning("Leaderboard '" + id + "' missing stat-key, skipping.");
            return null;
        }

        StatPeriod period;
        try {
            period = StatPeriod.valueOf(sec.getString("period", "ALLTIME").toUpperCase());
        } catch (IllegalArgumentException e) {
            period = StatPeriod.ALLTIME;
        }

        String displayName = sec.getString("display-name", "<gold>" + id + "</gold>");
        int maxEntries = sec.getInt("max-entries", 10);
        List<String> header = sec.getStringList("header");
        String entryFormat = sec.getString("entry-format", "<yellow>{position}. <white>{player} <gray>- <aqua>{value}");
        List<String> footer = sec.getStringList("footer");
        String emptyEntry = sec.getString("empty-entry", "<dark_gray>{position}. ---");

        Location hologramLoc = null;
        ConfigurationSection holoSec = sec.getConfigurationSection("hologram");
        if (holoSec != null) {
            String worldName = holoSec.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                hologramLoc = new Location(world,
                    holoSec.getDouble("x"), holoSec.getDouble("y"), holoSec.getDouble("z"),
                    (float) holoSec.getDouble("yaw"), (float) holoSec.getDouble("pitch"));
            }
        }

        Material icon = Material.DIAMOND_SWORD;
        try {
            String iconStr = sec.getString("icon");
            if (iconStr != null) icon = Material.valueOf(iconStr.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        return new LeaderboardConfig(id, statKey, period, displayName, maxEntries,
            hologramLoc, header, entryFormat, footer, emptyEntry, icon);
    }
}


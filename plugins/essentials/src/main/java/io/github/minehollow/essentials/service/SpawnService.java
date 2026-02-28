package io.github.minehollow.essentials.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the server spawn point, persisted in config.yml.
 */
public class SpawnService {

    private final JavaPlugin plugin;
    private Location spawnLocation;

    public SpawnService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("spawn.x")) {
            String worldName = config.getString("spawn.world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) world = Bukkit.getWorlds().getFirst();

            spawnLocation = new Location(
                world,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")
            );
        }
    }

    public @Nullable Location getSpawn() {
        return spawnLocation;
    }

    public boolean isSet() {
        return spawnLocation != null;
    }

    public void setSpawn(@NotNull Location location) {
        this.spawnLocation = location.clone();
        FileConfiguration config = plugin.getConfig();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void reload() {
        load();
    }
}


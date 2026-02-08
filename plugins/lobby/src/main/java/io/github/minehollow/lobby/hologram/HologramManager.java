package io.github.minehollow.lobby.hologram;

import io.github.minehollow.lobby.LobbyPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HologramManager {
    private static final double DEFAULT_Y_OFFSET = 2.1;

    private final LobbyPlugin plugin;
    private final File dataFile;
    private final Map<String, HologramHandler> holograms = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private boolean dirty = false;
    private int autoSaveTaskId = -1;


    public HologramManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;

        this.dataFile = new File(plugin.getDataFolder(), "holograms.yml");
        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        this.autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) {
                save();
                dirty = false;
            }
        }, 6000L, 6000L).getTaskId();
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create holograms.yml: " + e.getMessage());
                return;
            }
        }

        var config = YamlConfiguration.loadConfiguration(dataFile);
        var hologramsSection = config.getConfigurationSection("holograms");

        if (hologramsSection == null) {
            plugin.getLogger().info("No holograms to load");
            return;
        }

        for (var key : hologramsSection.getKeys(false)) {
            var section = hologramsSection.getConfigurationSection(key);
            if (section == null) continue;

            var data = deserializeHologram(key, section);
            if (data == null) continue;

            var handler = new HologramHandler(data, miniMessage);
            holograms.put(key, handler);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, handler::spawn);
        }

        plugin.getLogger().info("Loaded " + holograms.size() + " holograms");
    }

    public void save() {
        var config = new YamlConfiguration();
        var hologramsSection = config.createSection("holograms");

        for (var entry : holograms.entrySet()) {
            var id = entry.getKey();
            var handler = entry.getValue();
            var data = handler.getData();

            var section = hologramsSection.createSection(id);
            serializeHologram(data, section);
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Saved " + holograms.size() + " holograms");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save holograms: " + e.getMessage());
        }
    }

    public void saveNow() {
        save();
        dirty = false;
    }

    private void markDirty() {
        this.dirty = true;
    }

    public void createHologram(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        var data = new HologramData(id, location, lines);
        var handler = new HologramHandler(data, miniMessage);

        handler.spawn();
        holograms.put(id, handler);
        markDirty();
    }

    public boolean removeHologram(@NotNull String id) {
        var handler = holograms.remove(id);
        if (handler == null) return false;

        handler.remove();
        markDirty();
        return true;
    }

    public boolean updateHologramLines(@NotNull String id, @NotNull List<String> lines) {
        var handler = holograms.get(id);
        if (handler == null) return false;

        handler.updateLines(lines);
        markDirty();
        return true;
    }

    public boolean teleportHologram(@NotNull String id, @NotNull Location location) {
        var handler = holograms.get(id);
        if (handler == null) return false;

        handler.teleport(location);
        markDirty();
        return true;
    }

    @Nullable
    public HologramHandler getHologram(@NotNull String id) {
        return holograms.get(id);
    }

    public Collection<HologramHandler> getAllHolograms() {
        return holograms.values();
    }

    public void addViewerToAll(@NotNull Player player) {
        holograms.values().forEach(handler -> handler.addViewer(player));
    }

    public void removeViewerFromAll(@NotNull Player player) {
        holograms.values().forEach(handler -> handler.removeViewer(player));
    }

    public void unloadAll() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        holograms.values().forEach(HologramHandler::remove);
        holograms.clear();
    }

    private void serializeHologram(@NotNull HologramData data, @NotNull ConfigurationSection section) {
        var location = data.location();
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Cannot serialize hologram " + data.id() + ": world is null");
            return;
        }

        var locSection = section.createSection("location");
        locSection.set("world", location.getWorld().getName());
        locSection.set("x", location.getX());
        locSection.set("y", location.getY());
        locSection.set("z", location.getZ());

        if (!data.lines().isEmpty()) {
            section.set("lines", data.lines());
        }
    }

    @Nullable
    private HologramData deserializeHologram(@NotNull String id, @NotNull ConfigurationSection section) {
        var locSection = section.getConfigurationSection("location");
        if (locSection == null) {
            plugin.getLogger().warning("Invalid location data for hologram: " + id);
            return null;
        }

        var worldName = locSection.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("Missing world for hologram: " + id);
            return null;
        }

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World not found for hologram: " + id);
            return null;
        }

        var x = locSection.getDouble("x");
        var y = locSection.getDouble("y");
        var z = locSection.getDouble("z");

        var location = new Location(world, x, y, z);
        var lines = section.getStringList("lines");

        return new HologramData(id, location, lines);
    }
}
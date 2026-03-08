package io.github.minehollow.npc.config;

import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.npc.api.NpcAction;
import io.github.minehollow.npc.api.NpcClickType;
import io.github.minehollow.npc.api.actions.BroadcastAction;
import io.github.minehollow.npc.api.actions.CommandAction;
import io.github.minehollow.npc.api.actions.MessageAction;
import io.github.minehollow.npc.api.actions.SoundAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
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
 * Loads/saves NPC data to {@code npcs.yml}. Saves are performed asynchronously
 * with a dirty flag to avoid redundant writes.
 */
public class NpcStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public NpcStorage(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
        if (!file.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
    }

    // ── Load (synchronous — called at startup) ───────────────

    public @NotNull Map<String, NpcConfig> loadAll() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection npcsSection = yaml.getConfigurationSection("npcs");
        if (npcsSection == null) return new ConcurrentHashMap<>();

        Map<String, NpcConfig> result = new ConcurrentHashMap<>();
        for (String id : npcsSection.getKeys(false)) {
            ConfigurationSection npcSec = npcsSection.getConfigurationSection(id);
            if (npcSec == null) continue;

            NpcConfig config = deserializeNpc(id, npcSec);
            if (config != null) {
                result.put(id, config);
            }
        }
        return result;
    }

    // ── Save (async) ─────────────────────────────────────────

    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Saves all NPCs if the dirty flag is set. Called by the auto-save task
     * or on plugin disable.
     */
    public void saveIfDirty(@NotNull Map<String, NpcConfig> configs) {
        if (!dirty.compareAndSet(true, false)) return;
        saveAll(configs);
    }

    /**
     * Forces an immediate async save.
     */
    public void saveAsync(@NotNull Map<String, NpcConfig> configs) {
        dirty.set(false);
        Map<String, NpcConfig> snapshot = new LinkedHashMap<>(configs);
        Tasks.runAsync(() -> doSave(snapshot));
    }

    /**
     * Blocking save — used on plugin disable.
     */
    public void saveAll(@NotNull Map<String, NpcConfig> configs) {
        doSave(new LinkedHashMap<>(configs));
    }

    private void doSave(@NotNull Map<String, NpcConfig> configs) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection npcsSection = yaml.createSection("npcs");

        for (var entry : configs.entrySet()) {
            serializeNpc(npcsSection.createSection(entry.getKey()), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save npcs.yml", e);
        }
    }

    // ── Serialization ────────────────────────────────────────

    private void serializeNpc(@NotNull ConfigurationSection sec, @NotNull NpcConfig config) {
        Location loc = config.getLocation();
        sec.set("location.world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        sec.set("location.x", loc.getX());
        sec.set("location.y", loc.getY());
        sec.set("location.z", loc.getZ());
        sec.set("location.yaw", (double) loc.getYaw());
        sec.set("location.pitch", (double) loc.getPitch());

        if (config.getSkinValue() != null) {
            sec.set("skin.value", config.getSkinValue());
            sec.set("skin.signature", config.getSkinSignature());
        }

        sec.set("scale", config.getScale());

        if (!config.getHologramLines().isEmpty()) {
            sec.set("hologram.lines", config.getHologramLines());
            sec.set("hologram.offset", config.getHologramOffset());
        }

        sec.set("click-type", config.getClickType().name());
        sec.set("look-at-nearest-player", config.isLookAtNearestPlayer());

        if (!config.getActions().isEmpty()) {
            List<Map<String, Object>> actionList = new ArrayList<>();
            for (NpcAction action : config.getActions()) {
                actionList.add(serializeAction(action));
            }
            sec.set("actions", actionList);
        }
    }

    private @NotNull Map<String, Object> serializeAction(@NotNull NpcAction action) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", action.getType());
        switch (action) {
            case CommandAction cmd -> {
                map.put("executor", cmd.getExecutor().name());
                map.put("command", cmd.getCommand());
            }
            case MessageAction msg -> map.put("message", msg.getMessage());
            case SoundAction snd -> {
                map.put("sound", snd.getSound().getKey().getKey());
                map.put("volume", snd.getVolume());
                map.put("pitch", snd.getPitch());
            }
            case BroadcastAction bc -> map.put("message", bc.getMessage());
            default -> { /* custom actions won't be saved unless extended */ }
        }
        return map;
    }

    // ── Deserialization ──────────────────────────────────────

    private NpcConfig deserializeNpc(@NotNull String id, @NotNull ConfigurationSection sec) {
        ConfigurationSection locSec = sec.getConfigurationSection("location");
        if (locSec == null) return null;

        String worldName = locSec.getString("world", "world");
        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("NPC '" + id + "' references unknown world '" + worldName + "', skipping.");
            return null;
        }

        Location loc = new Location(world,
            locSec.getDouble("x"), locSec.getDouble("y"), locSec.getDouble("z"),
            (float) locSec.getDouble("yaw"), (float) locSec.getDouble("pitch"));

        String skinValue = null, skinSignature = null;
        ConfigurationSection skinSec = sec.getConfigurationSection("skin");
        if (skinSec != null) {
            skinValue = skinSec.getString("value");
            skinSignature = skinSec.getString("signature");
        }

        double scale = sec.getDouble("scale", 1.0);

        List<String> holoLines = new ArrayList<>();
        double holoOffset = 2.2;
        ConfigurationSection holoSec = sec.getConfigurationSection("hologram");
        if (holoSec != null) {
            holoLines = new ArrayList<>(holoSec.getStringList("lines"));
            holoOffset = holoSec.getDouble("offset", 2.2);
        }

        NpcClickType clickType = NpcClickType.RIGHT;
        try {
            clickType = NpcClickType.valueOf(sec.getString("click-type", "RIGHT").toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        boolean lookAtNearestPlayer = sec.getBoolean("look-at-nearest-player", false);

        List<NpcAction> actions = new ArrayList<>();
        List<?> actionList = sec.getList("actions");
        if (actionList != null) {
            for (Object obj : actionList) {
                if (obj instanceof Map<?, ?> map) {
                    NpcAction action = deserializeAction(map);
                    if (action != null) actions.add(action);
                }
            }
        }

        return new NpcConfig(id, loc, skinValue, skinSignature, scale, holoLines, holoOffset, actions, clickType, lookAtNearestPlayer);
    }

    private NpcAction deserializeAction(@NotNull Map<?, ?> map) {
        String type = String.valueOf(map.get("type")).toUpperCase();
        return switch (type) {
            case "COMMAND" -> {
                CommandAction.Executor executor = CommandAction.Executor.CONSOLE;
                try {
                    executor = CommandAction.Executor.valueOf(String.valueOf(map.get("executor")).toUpperCase());
                } catch (IllegalArgumentException ignored) {}
                yield new CommandAction(executor, String.valueOf(map.get("command")));
            }
            case "MESSAGE" -> new MessageAction(String.valueOf(map.get("message")));
            case "SOUND" -> {
                Sound sound;
                try {
                    String key = String.valueOf(map.get("sound")).toLowerCase().replace('_', '.');
                    sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
                    if (sound == null) {
                        // Fallback: try raw key
                        sound = Registry.SOUNDS.get(NamespacedKey.minecraft(String.valueOf(map.get("sound")).toLowerCase()));
                    }
                    if (sound == null) yield null;
                } catch (Exception e) {
                    yield null;
                }
                float volume = map.containsKey("volume") ? ((Number) map.get("volume")).floatValue() : 1.0f;
                float pitch = map.containsKey("pitch") ? ((Number) map.get("pitch")).floatValue() : 1.0f;
                yield new SoundAction(sound, volume, pitch);
            }
            case "BROADCAST" -> new BroadcastAction(String.valueOf(map.get("message")));
            default -> null;
        };
    }
}

package io.github.minehollow.lobby.npc;

import io.github.minehollow.lobby.LobbyPlugin;
import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.Cooldown;
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
import java.util.Map;

public class NPCManager {
    private static final String STEVE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMTc2OTI3NzYyMCwKICAicHJvZmlsZUlkIiA6ICJkZTU3MTIyNGUzODk0NjhiYTM3ZjE4ZDk1ZjQzZTNiOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfU3RldmUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGMzYzk3YTRhZjRhNTBjYjRmYjc5MzVjYzFiZGU0ZjE1YmJjZGJhODVlY2Q4NjkxYTI2OTQ0YWJkMmU3NzMzYiIKICAgIH0KICB9Cn0=";
    private static final String STEVE_SIGNATURE = "";
    private static final double HOLOGRAM_Y_OFFSET = 2.1;

    private final LobbyPlugin plugin;
    private final HologramManager hologramManager;
    private final File dataFile;
    private final Map<String, NPCHandler> npcs = new HashMap<>();
    private final Map<Integer, NPCHandler> npcsByEntityId = new HashMap<>();

    private boolean dirty = false;
    private int autoSaveTaskId = -1;

    public NPCManager(@NotNull LobbyPlugin plugin, @NotNull HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.dataFile = new File(plugin.getDataFolder(), "npcs.yml");
        startAutoSave();
    }

    private void startAutoSave() {
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
                plugin.getLogger().severe("Failed to create npcs.yml: " + e.getMessage());
                return;
            }
        }

        var config = YamlConfiguration.loadConfiguration(dataFile);
        var npcsSection = config.getConfigurationSection("npcs");

        if (npcsSection == null) {
            plugin.getLogger().info("No NPCs to load");
            return;
        }

        for (var key : npcsSection.getKeys(false)) {
            var section = npcsSection.getConfigurationSection(key);
            if (section == null) continue;

            var data = deserializeNPC(key, section);
            if (data == null) continue;

            var handler = new NPCHandler(data, hologramManager);
            npcs.put(key, handler);

            Bukkit.getScheduler().runTask(plugin, () -> {
                handler.spawn();
                npcsByEntityId.put(handler.getNpcEntity().getEntityId(), handler);
            });
        }

        plugin.getLogger().info("Loaded " + npcs.size() + " NPCs");
    }

    public void save() {
        var config = new YamlConfiguration();
        var npcsSection = config.createSection("npcs");

        for (var entry : npcs.entrySet()) {
            var name = entry.getKey();
            var handler = entry.getValue();
            var data = handler.getData();

            var section = npcsSection.createSection(name);
            serializeNPC(data, section);
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Saved " + npcs.size() + " NPCs");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save NPCs: " + e.getMessage());
        }
    }

    public void saveNow() {
        save();
        dirty = false;
    }

    private void markDirty() {
        this.dirty = true;
    }

    public void createNPC(@NotNull String name, @NotNull Location location) {
        var data = new NPCData(name, location, STEVE_TEXTURE, STEVE_SIGNATURE, null, null);
        var handler = new NPCHandler(data, hologramManager);

        handler.spawn();
        npcs.put(name, handler);
        npcsByEntityId.put(handler.getNpcEntity().getEntityId(), handler);
        markDirty();
    }

    public boolean removeNPC(@NotNull String name) {
        var handler = npcs.remove(name);
        if (handler == null) return false;

        npcsByEntityId.remove(handler.getNpcEntity().getEntityId());
        handler.remove();
        markDirty();
        return true;
    }

    /**
     * Define a skin do NPC usando a skin de um jogador
     */
    public boolean setSkin(@NotNull String npcName, @NotNull Player skinSource) {
        var handler = npcs.get(npcName);
        if (handler == null) {
            return false;
        }

        var profile = skinSource.getPlayerProfile();
        var textures = profile.getProperties().stream()
          .filter(prop -> prop.getName().equals("textures"))
          .findFirst();

        if (textures.isEmpty()) {
            return false;
        }

        var prop = textures.get();
        var value = prop.getValue();
        var signature = prop.getSignature();
        if (signature == null) signature = "";

        return setSkinByData(npcName, value, signature);
    }

    /**
     * Define a skin do NPC usando dados de textura diretos
     */
    public boolean setSkinByData(@NotNull String npcName, @NotNull String texture, @NotNull String signature) {
        var handler = npcs.get(npcName);
        if (handler == null) {
            return false;
        }

        npcsByEntityId.remove(handler.getNpcEntity().getEntityId());

        handler.updateSkin(texture, signature);

        // Add new entity ID mapping after skin update
        npcsByEntityId.put(handler.getNpcEntity().getEntityId(), handler);

        markDirty();
        return true;
    }

    public boolean attachHologram(@NotNull String npcName, @NotNull String hologramId) {
        var handler = npcs.get(npcName);
        if (handler == null) return false;

        var hologram = hologramManager.getHologram(hologramId);
        if (hologram == null) return false;

        handler.setHologram(hologramId);

        var hologramLocation = handler.getData().location().clone();
        hologramLocation.add(0, HOLOGRAM_Y_OFFSET, 0);
        hologramManager.teleportHologram(hologramId, hologramLocation);

        markDirty();
        return true;
    }

    public boolean detachHologram(@NotNull String npcName) {
        var handler = npcs.get(npcName);
        if (handler == null) return false;

        handler.setHologram(null);
        markDirty();
        return true;
    }

    public boolean teleportNPC(@NotNull String npcName, @NotNull Location location) {
        var handler = npcs.get(npcName);
        if (handler == null) return false;

        handler.teleport(location);
        markDirty();
        return true;
    }

    public boolean setInteraction(@NotNull String npcName, @NotNull String interaction) {
        var handler = npcs.get(npcName);
        if (handler == null) return false;

        handler.updateInteraction(interaction);
        markDirty();
        return true;
    }

    public void handleInteraction(@NotNull Player player, int entityId) {
        final var handler = getNPCByEntityId(entityId);
        if (handler == null) return;

        final var data = handler.getData();
        if (data.interaction() == null || data.interaction().isBlank()) return;

        if (Cooldown.isInCooldown(player.getUniqueId(), "npc_interaction")) {
            return;
        }


        Cooldown.setCooldownSec(player.getUniqueId(), 2L, "npc_interaction");

        var interaction = data.interaction().replace("{player}", player.getName());

        if (interaction.startsWith("server:")) {
            var server = interaction.substring(7).trim();
            sendPlayerToServer(player, server);
        } else if (interaction.startsWith("command:")) {
            var command = interaction.substring(8).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            // Backward compatibility: assume it's a server name
            sendPlayerToServer(player, interaction.trim());
        }
    }

    private void sendPlayerToServer(@NotNull Player player, @NotNull String server) {
        BukkitPlatform.getInstance()
          .tryConnectPlayerToServer(player.getUniqueId(), server);
    }

    public void addViewerToAll(@NotNull Player player) {
        npcs.values().forEach(handler -> handler.addViewer(player));
    }

    public void removeViewerFromAll(@NotNull Player player) {
        npcs.values().forEach(handler -> handler.removeViewer(player));
    }

    public void unloadAll() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        npcs.values().forEach(NPCHandler::remove);
        npcs.clear();
        npcsByEntityId.clear();
    }

    @Nullable
    public NPCHandler getNPC(@NotNull String name) {
        return npcs.get(name);
    }

    @Nullable
    public NPCHandler getNPCByEntityId(int entityId) {
        return npcsByEntityId.get(entityId);
    }

    public Collection<NPCHandler> getAllNPCs() {
        return npcs.values();
    }

    private void serializeNPC(@NotNull NPCData data, @NotNull ConfigurationSection section) {
        var location = data.location();
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Cannot serialize NPC " + data.name() + ": world is null");
            return;
        }

        var locSection = section.createSection("location");
        locSection.set("world", location.getWorld().getName());
        locSection.set("x", location.getX());
        locSection.set("y", location.getY());
        locSection.set("z", location.getZ());
        locSection.set("yaw", location.getYaw());
        locSection.set("pitch", location.getPitch());

        if (data.skinTexture() != null && data.skinSignature() != null) {
            var skinSection = section.createSection("skin");
            skinSection.set("texture", data.skinTexture());
            skinSection.set("signature", data.skinSignature());
        }

        if (data.hologramId() != null) {
            section.set("hologram", data.hologramId());
        }

        if (data.interaction() != null) {
            section.set("interaction", data.interaction());
        }
    }

    @Nullable
    private NPCData deserializeNPC(@NotNull String name, @NotNull ConfigurationSection section) {
        var locSection = section.getConfigurationSection("location");
        if (locSection == null) {
            plugin.getLogger().warning("Invalid location data for NPC: " + name);
            return null;
        }

        var worldName = locSection.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("Missing world for NPC: " + name);
            return null;
        }

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World not found for NPC: " + name);
            return null;
        }

        var x = locSection.getDouble("x");
        var y = locSection.getDouble("y");
        var z = locSection.getDouble("z");
        var yaw = (float) locSection.getDouble("yaw");
        var pitch = (float) locSection.getDouble("pitch");

        var location = new Location(world, x, y, z, yaw, pitch);

        String skinTexture = null;
        String skinSignature = null;
        var skinSection = section.getConfigurationSection("skin");
        if (skinSection != null) {
            skinTexture = skinSection.getString("texture");
            skinSignature = skinSection.getString("signature");
        }

        var hologramId = section.getString("hologram");
        var interaction = section.getString("interaction");

        return new NPCData(name, location, skinTexture, skinSignature, hologramId, interaction);
    }
}
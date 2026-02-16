package io.github.minehollow.lobby;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.lobby.command.HologramCommand;
import io.github.minehollow.lobby.command.NPCCommand;
import io.github.minehollow.lobby.command.SpawnCommand;
import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.lobby.listener.*;
import io.github.minehollow.lobby.menu.ServerMenu;
import io.github.minehollow.lobby.npc.NPCManager;
import io.github.minehollow.lobby.service.SkinService;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class LobbyPlugin extends SimplePlugin {


    public static ItemStack SERVER_SELECTOR;


    private ExecutorService asyncExecutor;
    private HologramManager hologramManager;
    private NPCManager npcManager;

    private Location spawnLocation;

    private SkinService skinService;

    @Override
    public void onEnable() {

        SERVER_SELECTOR = ItemBuilder.of(Material.COMPASS)
          .name("<gold>Seletor de Servidores")
          .lore(
            "<gray>Clique para escolher um servidor!"
          )
          .build();

        this.saveDefaultConfig();
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
        APIConfig settings = new APIConfig(PacketEvents.getAPI())
          .debugMode()
          .tickTickables()
          .usePlatformLogger();

        EntityLib.init(platform, settings);

        spawnLocation = getServer().getWorlds().getFirst().getSpawnLocation();
        if (getConfig().contains("spawn-location")) {
            spawnLocation = getConfig().getLocation("spawn-location");
        }


        hologramManager = new HologramManager(this);
        hologramManager.load();

        skinService = new SkinService();

        npcManager = new NPCManager(this, hologramManager);
        npcManager.load();


        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, hologramManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(hologramManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInventoryListener(), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        PacketEvents.getAPI().getEventManager()
          .registerListener(new NPCClickListener(this, npcManager));


        registerCommands("spawn", new SpawnCommand(this));
        registerCommands("npc", new NPCCommand(npcManager, skinService));
        registerCommands("hologram", new HologramCommand(hologramManager));

        MenuUtil.registerMenus(new ServerMenu());

    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.saveNow();
            npcManager.unloadAll();
        }

        if (hologramManager != null) {
            hologramManager.saveNow();
            hologramManager.unloadAll();
        }



        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        PacketEvents.getAPI().terminate();

        asyncExecutor.shutdown();
    }

    public void setSpawnLocationAndSave(Location location) {
        this.spawnLocation = location;
        getConfig().set("spawn-location", location);
        saveConfig();
    }

}
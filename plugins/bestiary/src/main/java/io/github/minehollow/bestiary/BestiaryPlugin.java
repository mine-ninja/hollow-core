package io.github.minehollow.bestiary;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.bestiary.archetype.MobArchetypeService;
import io.github.minehollow.bestiary.command.MobEditCommand;
import io.github.minehollow.bestiary.custom.CustomMobManager;
import io.github.minehollow.bestiary.menu.ArchetypeEditMenu;
import io.github.minehollow.bestiary.menu.ArchetypeListMenu;
import io.github.minehollow.bestiary.spawner.CustomMobSpawner;
import io.github.minehollow.bestiary.spawner.SpawnerService;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;

@Getter
public class BestiaryPlugin extends SimplePlugin {

    private MobArchetypeService mobArchetypeService;
    private SpawnerService spawnerService;
    private CustomMobManager customMobManager;


    @Override
    public void onEnable() {
        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
        APIConfig settings = new APIConfig(PacketEvents.getAPI()).usePlatformLogger();

        EntityLib.init(platform, settings);

        this.mobArchetypeService = new MobArchetypeService();
        this.mobArchetypeService.loadAllArchetypes();

        this.spawnerService = new SpawnerService(this);
        this.spawnerService.preCacheAllSpawners();

        this.customMobManager = new CustomMobManager(this);

        MenuUtil.registerMenus(
            new ArchetypeListMenu(this),
            new ArchetypeEditMenu(this)
        );

        registerCommands("mobedit", new MobEditCommand(this));
    }
}

package io.github.minehollow.bestiary;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.bestiary.command.MonsterCommand;
import io.github.minehollow.bestiary.command.SpawnerCommand;
import io.github.minehollow.bestiary.display.DamageIndicator;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import io.github.minehollow.bestiary.monster.MonsterListener;
import io.github.minehollow.bestiary.monster.MonsterManager;
import io.github.minehollow.bestiary.monster.packet.MonsterPacketListener;
import io.github.minehollow.bestiary.spawner.SpawnerManager;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;

@Getter
public class BestiaryPlugin extends SimplePlugin {

    private CustomMonsterModelManager customMonsterModelManager;


    private MonsterManager monsterManager;
    private SpawnerManager spawnerManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
        APIConfig settings = new APIConfig(PacketEvents.getAPI()).usePlatformLogger();

        EntityLib.init(platform, settings);

        this.customMonsterModelManager = new CustomMonsterModelManager(this);
        this.customMonsterModelManager.loadModels();

        this.monsterManager = new MonsterManager(this, customMonsterModelManager);
        this.spawnerManager = new SpawnerManager(this, monsterManager);

        DamageIndicator.init(this);

        this.registerListeners(
            new MonsterListener(monsterManager)
        );

        this.registerCommands(
            "bestiary",
            new MonsterCommand(customMonsterModelManager, monsterManager),
            new SpawnerCommand(spawnerManager, customMonsterModelManager)
        );

        PacketEvents.getAPI().getEventManager().registerListener(new MonsterPacketListener(this.monsterManager));
    }

    @Override
    public void onDisable() {
        DamageIndicator.shutdown();
    }
}

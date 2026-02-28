package io.github.minehollow.essentials;

import io.github.minehollow.essentials.command.*;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.essentials.listener.EssentialsListener;
import io.github.minehollow.essentials.service.HomeService;
import io.github.minehollow.essentials.service.SpawnService;
import io.github.minehollow.essentials.service.TeleportService;
import io.github.minehollow.essentials.service.TpaService;
import io.github.minehollow.essentials.tablist.TabListManager;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class EssentialsPlugin extends SimplePlugin {

    private MessageConfig messageConfig;
    private SpawnService spawnService;
    private HomeService homeService;
    private TpaService tpaService;
    private TeleportService teleportService;
    private TabListManager tabListManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Config & messages
        this.messageConfig = new MessageConfig(this);

        // Services
        this.spawnService = new SpawnService(this);
        this.homeService = new HomeService();

        int tpaTimeout = getConfig().getInt("teleport.request-timeout", 60);
        this.tpaService = new TpaService(tpaTimeout);

        int tpDelay = getConfig().getInt("teleport.delay", 3);
        boolean cancelOnMove = getConfig().getBoolean("teleport.cancel-on-move", true);
        this.teleportService = new TeleportService(tpDelay, cancelOnMove, messageConfig);

        // TabList
        this.tabListManager = new TabListManager(this);
        tabListManager.start();

        // Commands
        registerCommands("essentials",
            new SpawnCommand(this),
            new SetSpawnCommand(this),
            new TpCommand(this),
            new TpHereCommand(this),
            new TpaCommand(this),
            new TpAcceptCommand(this),
            new TpDenyCommand(this),
            new HomeCommand(this),
            new SetHomeCommand(this),
            new DelHomeCommand(this),
            new HomesCommand(this),
            new EnderChestCommand(this),
            new CraftCommand(this),
            new HollowCoreCommand(this)
        );

        // Listeners
        registerListeners(new EssentialsListener(this), teleportService);

        log.info("EssentialsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (tabListManager != null) {
            tabListManager.stop();
        }
        log.info("EssentialsPlugin disabled.");
    }

    /**
     * Reloads config.yml, messages.yml, spawn, and tab list.
     */
    public void reloadAll() {
        reloadConfig();
        messageConfig.reload();
        spawnService.reload();
        tabListManager.reload();
    }
}


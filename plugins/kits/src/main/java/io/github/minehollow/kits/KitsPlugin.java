package io.github.minehollow.kits;

import io.github.minehollow.kits.command.KitCommand;
import io.github.minehollow.kits.listener.PlayerJoinListener;
import io.github.minehollow.kits.listener.PlayerQuitListener;
import io.github.minehollow.kits.menu.KitCategoryMenu;
import io.github.minehollow.kits.menu.KitEditorMenu;
import io.github.minehollow.kits.menu.KitListMenu;
import io.github.minehollow.kits.menu.KitPreviewMenu;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.menu.input.SignInputMenu;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;

@Getter
public class KitsPlugin extends SimplePlugin {
    private KitRepository kitRepository;
    private KitService kitService;

    @Override
    public void onEnable() {
        this.kitRepository = new KitRepository(this);
        this.kitService = new KitService(this.kitRepository);

        try {
            this.kitRepository.connect();
            this.kitService.loadAllCategories();
            this.kitService.loadAllKits();
            getLogger().info("Successfully loaded kits and categories into cache.");
        } catch (Exception e) {
            getLogger().severe("Disabling plugin due to MongoDB connection failure!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BukkitPlatform platform = BukkitPlatform.getInstance();
        platform.getMenuManager().register(
            new SignInputMenu(),
            new KitCategoryMenu(kitService),
            new KitListMenu(kitService),
            new KitPreviewMenu(),
            new KitEditorMenu(kitService));

        registerCommands("kits", new KitCommand(kitService, platform));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(kitService), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(kitService), this);

        getLogger().info("Kits plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (this.kitRepository != null) {
            this.kitRepository.close();
        }
    }
}

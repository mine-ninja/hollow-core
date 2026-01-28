package io.github.minehollow.kits;

import io.github.minehollow.kits.kit.KitRepository;
import io.github.minehollow.kits.kit.KitService;
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
            this.kitService.loadAllKits();
            getLogger().info("Successfully loaded kits into cache.");
        } catch (Exception e) {
            getLogger().severe("Disabling plugin due to MongoDB connection failure!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.kitRepository != null) {
            this.kitRepository.close();
        }
    }
}

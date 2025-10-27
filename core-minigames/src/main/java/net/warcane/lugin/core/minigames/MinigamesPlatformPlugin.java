package net.warcane.lugin.core.minigames;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MinigamesPlatformPlugin extends JavaPlugin {

    @Getter
    private static MinigamesPlatformPlugin instance;

    private MinigamesPlatform minigamesPlatform;

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;

        this.adventure = BukkitAudiences.create(this);

        minigamesPlatform = MinigamesPlatform.provide(this);
        minigamesPlatform.init();
    }

    @Override
    public void onDisable() {
        if (minigamesPlatform != null) {
            minigamesPlatform.close();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public @NotNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }
}

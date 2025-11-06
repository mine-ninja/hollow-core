package net.warcane.lugin.core.minigames.internal.listeners;

import net.warcane.lugin.core.minigames.MinigamesPlatform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public record InternalListener(MinigamesPlatform platform, JavaPlugin plugin) {

    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PartyListeners(platform), plugin);
    }
}

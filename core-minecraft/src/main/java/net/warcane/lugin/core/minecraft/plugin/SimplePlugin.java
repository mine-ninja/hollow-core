package net.warcane.lugin.core.minecraft.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SimplePlugin extends JavaPlugin {

    public void registerCommands(@NotNull String prefix, @NotNull Command... commands) {
        Bukkit.getCommandMap().registerAll(prefix, List.of(commands));
    }

    public void registerListeners(@NotNull Listener... listener) {
        for (Listener l : listener) {
            Bukkit.getPluginManager().registerEvents(l, this);
        }
    }
}

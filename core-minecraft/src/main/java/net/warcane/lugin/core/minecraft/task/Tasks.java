package net.warcane.lugin.core.minecraft.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Tasks {

    private static Plugin plugin;

    // todo achar uma forma de fazer isso de uma forma menos nojenta.
    private static Plugin findFirstPlugin() {
        if (plugin != null) {
            return plugin;
        }

        return plugin = Arrays.stream(Bukkit.getPluginManager().getPlugins())
          .filter(Plugin::isEnabled)
          .findFirst()
          .orElse(null);
    }

    public static void runSyncLater(@NotNull Runnable runnable, long delay) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the task later.");
        }
    }

    public static void runSync(@NotNull Runnable runnable) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the task synchronously.");
        }
    }

    public static void runSyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the repeating task synchronously.");
        }
    }

    public static void runAsync(@NotNull Runnable runnable) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the task asynchronously.");
        }
    }

    public static void runAsyncLater(@NotNull Runnable runnable, long delay) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the task asynchronously later.");
        }
    }

    public static void runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
        } else {
            throw new IllegalStateException("No enabled plugin found to run the repeating task asynchronously.");
        }
    }
}

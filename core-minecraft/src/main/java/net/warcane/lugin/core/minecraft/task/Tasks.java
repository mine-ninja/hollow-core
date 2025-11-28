package net.warcane.lugin.core.minecraft.task;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Tasks {

    private static Plugin plugin;
    private static FoliaLib foliaLib;

    public static void initialize(@NotNull Plugin plugin) {
        plugin = Arrays.stream(Bukkit.getPluginManager().getPlugins())
          .filter(Plugin::isEnabled)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No enabled plugin found."));

        foliaLib = new FoliaLib(plugin);
    }


    public static void runSyncLater(@NotNull Runnable runnable, long delay) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runLater(runnable, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static void runSync(@NotNull Runnable runnable) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runNextTick(task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runSyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runTimer(runnable, delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }

    public static void runAsync(@NotNull Runnable runnable) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runAsync(task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runAsyncLater(@NotNull Runnable runnable, long delay) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runLaterAsync(runnable, delay);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
    }

    public static void runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runTimerAsync(runnable, delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
        }
    }


    public static boolean isFolia() {
        return foliaLib.isFolia();
    }
}
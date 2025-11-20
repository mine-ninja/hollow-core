package net.warcane.lugin.core.minecraft.task;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

public class Tasks {

    private static Plugin plugin;
    private static FoliaLib foliaLib;

    // todo achar uma forma de fazer isso de uma forma menos nojenta.
    private static Plugin findFirstPlugin() {
        if (plugin != null) {
            return plugin;
        }

        final var foundPlugin = plugin = Arrays.stream(Bukkit.getPluginManager().getPlugins())
            .filter(Plugin::isEnabled)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No enabled plugin found."));

        if (foliaLib == null) {
            foliaLib = new FoliaLib(foundPlugin);
        }

        return plugin;
    }

    private static boolean isFolia() {
        return foliaLib != null && foliaLib.isFolia();
    }


    public static void runSyncLater(@NotNull Runnable runnable, long delay) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
                return;
            }

            foliaLib.getScheduler().runLater(runnable, delay);
        });
    }


    public static void runSync(@NotNull Runnable runnable) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            } else {
                foliaLib.getScheduler().runNextTick(task -> runnable.run());
            }
        });
    }

    public static void runSyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
                return;
            }

            foliaLib.getScheduler().runTimer(runnable, delay, period);
        });
    }

    public static void runAsync(@NotNull Runnable runnable) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
                return;
            }


            foliaLib.getScheduler().runAsync(task -> runnable.run());
        });
    }

    public static void runAsyncLater(@NotNull Runnable runnable, long delay) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
                return;
            }

            foliaLib.getScheduler().runLaterAsync(runnable, delay);
        });
    }

    public static void runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        usePlugin(plugin -> {
            if (!isFolia()) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
                return;
            }

            foliaLib.getScheduler().runTimerAsync(runnable, delay, period);
        });
    }

    public static void usePlugin(@NotNull Consumer<Plugin> consumer) {
        Plugin plugin = findFirstPlugin();
        if (plugin != null) {
            consumer.accept(plugin);
        } else {
            throw new IllegalStateException("No enabled plugin found to use.");
        }
    }
}

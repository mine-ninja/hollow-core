package net.warcane.lugin.core.minecraft.task;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class Tasks {
    private static Plugin plugin;
    private static FoliaLib foliaLib;
    private static boolean initialized = false;

    public static void initialize(@NotNull Plugin plugin) {
        Tasks.plugin = plugin;
        foliaLib = new FoliaLib(Tasks.plugin);
        initialized = true;
    }

    private static void checkInit() {
        if (!initialized) {
            throw new IllegalStateException("Tasks not initialized. Call Tasks.initialize(plugin) from your plugin's onEnable.");
        }
    }
    
    public static void runSync(@NotNull Runnable runnable) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runNextTick(task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runSyncLater(@NotNull Runnable runnable, long delay) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runLater(runnable, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static void runSyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runTimer(runnable, delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }

    public static void runAsync(@NotNull Runnable runnable) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runAsync(task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runAsyncLater(@NotNull Runnable runnable, long delay) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runLaterAsync(runnable, delay);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
    }

    public static void runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        checkInit();
        if (foliaLib.isFolia()) {
            foliaLib.getScheduler().runTimerAsync(runnable, delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
        }
    }

    public static void runSyncAt(@NotNull Runnable runnable, Location location) {
        checkInit();
        foliaLib.getScheduler().runAtLocation(location, task -> runnable.run());
    }
    
    public static void runSyncLaterAt(@NotNull Runnable runnable, Location location, long delay) {
        checkInit();
        foliaLib.getScheduler().runAtLocationLater(location, runnable, delay);
    }
    
    public static void runSyncRepeatingAt(@NotNull Runnable runnable, Location location, long delay, long period) {
        checkInit();
        foliaLib.getScheduler().runAtLocationTimer(location, runnable, delay, period);
    }
    
    public static void runSyncFor(@NotNull Runnable runnable, Entity entity) {
        checkInit();
        foliaLib.getScheduler().runAtEntity(entity, task -> runnable.run());
    }
    
    public static void runSyncLaterFor(@NotNull Runnable runnable, Entity entity, long delay) {
        checkInit();
        foliaLib.getScheduler().runAtEntityLater(entity, runnable, delay);
    }
    
    public static void runSyncRepeatingFor(@NotNull Runnable runnable, Entity entity, long delay, long period) {
        checkInit();
        foliaLib.getScheduler().runAtEntityTimer(entity, runnable, delay, period);
    }

    public static void runLocationTask(Location location, @NotNull Runnable runnable) {
        checkInit();
        if (isFolia() && location != null && location.getWorld() != null) {
            try {
                foliaLib.getScheduler().runAtLocation(location, scheduledTask -> runnable.run());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling location task in Folia, falling back to global scheduler", e);
                runSync(runnable);
            }
            return;
        } else {
            runSync(runnable);
        }
    }

    public static void runLocationTaskLater(Location location, @NotNull Runnable runnable, long delayTicks) {
        checkInit();
        if (isFolia() && location != null && location.getWorld() != null) {
            try {
                foliaLib.getScheduler().runAtLocationLater(location, runnable, delayTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling location task in Folia, falling back to global scheduler", e);
                runSyncLater(runnable, delayTicks);
            }
            return;
        } else {
            runSyncLater(runnable, delayTicks);
        }
    }

    public static void runLocationTaskTimer(Location location, @NotNull Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (isFolia() && location != null && location.getWorld() != null) {
            try {
                foliaLib.getScheduler().runAtLocationTimer(location, runnable, delayTicks, periodTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling location task in Folia, falling back to global scheduler", e);
                runSyncRepeating(runnable, delayTicks, periodTicks);
            }
            return;
        } else {
            runSyncRepeating(runnable, delayTicks, periodTicks);
        }
    }

    public static void runEntityTask(Entity entity, @NotNull Runnable runnable) {
        checkInit();
        if (isFolia() && entity != null) {
            try {
                foliaLib.getScheduler().runAtEntity(entity, scheduledTask -> runnable.run());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling entity task in Folia, falling back to global scheduler", e);
                runSync(runnable);
            }
            return;
        } else {
            runSync(runnable);
        }
    }

    public static void runEntityTaskLater(Entity entity, @NotNull Runnable runnable, long delayTicks) {
        checkInit();
        if (isFolia() && entity != null) {
            try {
                foliaLib.getScheduler().runAtEntityLater(entity, runnable, delayTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling entity task in Folia, falling back to global scheduler", e);
                runSyncLater(runnable, delayTicks);
            }
            return;
        } else {
            runSyncLater(runnable, delayTicks);
        }
    }

    public static void runEntityTaskTimer(Entity entity, @NotNull Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (isFolia() && entity != null) {
            try {
                foliaLib.getScheduler().runAtEntityTimer(entity, runnable, delayTicks, periodTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling entity task in Folia, falling back to global scheduler", e);
                runSyncRepeating(runnable, delayTicks, periodTicks);
            }
            return;
        } else {
            runSyncRepeating(runnable, delayTicks, periodTicks);
        }
    }


    
    public static boolean isFolia() {
        return foliaLib != null && foliaLib.isFolia();
    }
}

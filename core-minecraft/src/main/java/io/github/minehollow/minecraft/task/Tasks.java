package io.github.minehollow.minecraft.task;

import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Slf4j
public class Tasks {
    private static final Plugin plugin;
    @Getter private static final boolean isFolia;
    
    static {
        plugin = BukkitPlatformPlugin.getInstance();
        
        // Check if we're running on Folia
        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
            plugin.getLogger().info("Folia detected! Using region-based threading system.");
        } catch (final ClassNotFoundException e) {
            plugin.getLogger().info("Running on standard Paper server.");
        } catch (Exception e) {
            plugin.getLogger().warning("Unexpected error while detecting server type: " + e.getMessage());
        }
        isFolia = foliaDetected;
    }
    
    public static WrappedTask runSync(@NotNull Runnable runnable) {
        if (isFolia) {
            try {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run());
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTask(plugin, runnable));
        }
    }
    
    public static WrappedTask runSyncLater(@NotNull Runnable runnable, long delay) {
        if (isFolia) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling delayed task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        }
    }
    
    public static WrappedTask runSyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling timer task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        }
    }
    
    public static WrappedTask runAsync(@NotNull Runnable runnable) {
        if (isFolia) {
            try {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling async task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        }
    }
    
    public static WrappedTask runAsyncLater(@NotNull Runnable runnable, long delay) {
        if (isFolia) {
            try {
                long delayMs = delay * 50; // Convert ticks to milliseconds
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delayMs, TimeUnit.MILLISECONDS);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling delayed async task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
        }
    }
    
    public static WrappedTask runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            try {
                // Convert ticks to milliseconds (1 tick = 50ms)
                long delayMs = delay * 50;
                long periodMs = period * 50;
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error scheduling timer async task in Folia", e);
                return new WrappedTask(null);
            }
        } else {
            return new WrappedTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
        }
    }
    
    public static WrappedTask runAtLocation(@NotNull Runnable runnable, Location location) {
        if (isFolia && location != null && location.getWorld() != null) {
            try {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling location task in Folia, falling back to global scheduler", e);
                return runSync(runnable);
            }
        } else {
            return runSync(runnable);
        }
    }
    
    public static WrappedTask runAtLocationLater(@NotNull Runnable runnable, Location location, long delay) {
        if (isFolia && location != null && location.getWorld() != null) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), delay);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling delayed location task in Folia, falling back to global scheduler", e);
                return runSyncLater(runnable, delay);
            }
        } else {
            return runSyncLater(runnable, delay);
        }
    }
    
    public static WrappedTask runAtLocationRepeating(@NotNull Runnable runnable, Location location, long delay, long period) {
        if (isFolia && location != null && location.getWorld() != null) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), delay, period);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling timer location task in Folia, falling back to global scheduler", e);
                return runSyncRepeating(runnable, delay, period);
            }
        } else {
            return runSyncRepeating(runnable, delay, period);
        }
    }
    
    public static WrappedTask runAtEntity(@NotNull Runnable runnable, Entity entity) {
        if (isFolia && entity != null) {
            try {
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling entity task in Folia, falling back to global scheduler", e);
                return runSync(runnable);
            }
        } else {
            return runSync(runnable);
        }
    }
    
    public static WrappedTask runAtEntityLater(@NotNull Runnable runnable, Entity entity, long delay) {
        if (isFolia && entity != null) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, delay);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling delayed entity task in Folia, falling back to global scheduler", e);
                return runSyncLater(runnable, delay);
            }
        } else {
            return runSyncLater(runnable, delay);
        }
    }
    
    public static WrappedTask runAtEntityRepeating(@NotNull Runnable runnable, Entity entity, long delay, long period) {
        if (isFolia && entity != null) {
            try {
                delay = Math.max(1, delay);
                io.papermc.paper.threadedregions.scheduler.ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), null, delay, period);
                return new WrappedTask(task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error scheduling timer entity task in Folia, falling back to global scheduler", e);
                return runSyncRepeating(runnable, delay, period);
            }
        } else {
            return runSyncRepeating(runnable, delay, period);
        }
    }
}

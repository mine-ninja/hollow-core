package net.warcane.lugin.core.minecraft.task;

import org.bukkit.scheduler.BukkitTask;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper class for both Bukkit and Folia tasks.
 */
@Slf4j @Getter
public class WrappedTask {
    private final Object task;
    
    /**
     * Creates a new WrappedTask.
     *
     * @param task The underlying task object
     */
    WrappedTask(Object task) {
        this.task = task;
    }
    
    /**
     * Cancels the task.
     */
    public void cancel() {
        if (task == null) return;
        
        try {
            if (Tasks.isFolia()) {
                if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                    ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
                }
            } else {
                if (task instanceof BukkitTask) {
                    ((BukkitTask) task).cancel();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cancel task", e);
        }
    }
    
    /**
     * Checks if this task is canceled.
     *
     * @return true if the task is canceled
     */
    public boolean isCancelled() {
        if (task == null) return true;
        
        try {
            if (Tasks.isFolia()) {
                if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                    return ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).isCancelled();
                }
            } else {
                if (task instanceof BukkitTask) {
                    return ((BukkitTask) task).isCancelled();
                }
            }
        } catch (Exception ignored) {
            // WrappedTask may have already been garbage collected or is invalid
        }
        return true;
    }
}


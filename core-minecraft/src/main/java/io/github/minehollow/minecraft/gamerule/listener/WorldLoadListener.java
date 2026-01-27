package io.github.minehollow.minecraft.gamerule.listener;

import io.github.minehollow.minecraft.BukkitPlatform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit event listener for initializing game rules when worlds are loaded.
 * <p>
 * Ensures that dynamically loaded worlds have their game rules initialized.
 */
public class WorldLoadListener implements Listener {
    
    private final BukkitPlatform platform;
    
    public WorldLoadListener(@NotNull BukkitPlatform platform) {
        this.platform = platform;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        platform.getGameRuleManager().initializeWorld(event.getWorld());
    }
}

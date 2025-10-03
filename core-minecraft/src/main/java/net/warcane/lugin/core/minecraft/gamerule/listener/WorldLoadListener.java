package net.warcane.lugin.core.minecraft.gamerule.listener;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listener to initialize custom game rules storage when worlds are loaded.
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

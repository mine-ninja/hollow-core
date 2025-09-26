package net.warcane.lugin.core.minecraft.menu;

import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.util.stopwatch.Stopwatch;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Rok, Pedro Lucas nmm. Created on 05/09/2025
 * @project lugin-core
 */
public interface MenuContext extends InventoryHolder {
    MenuConfig getMenuConfig();
    
    Stopwatch getStopwatch();
    
    void update();
}

package io.github.minehollow.minecraft.menu;

import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import org.bukkit.inventory.InventoryHolder;


public interface MenuContext extends InventoryHolder {
    MenuConfig getMenuConfig();
    
    Stopwatch getStopwatch();
    
    void update();

    void updateSlot(int slot);
}

package net.warcane.lugin.core.minecraft.menu;

import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.pagination.MenuPaginationContext;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.Map;

/**
 * @author Rok, Pedro Lucas nmm. Created on 06/09/2025
 * @project lugin-core
 */
public abstract class SimplePaginationMenu<T> extends SimpleMenu {
    public boolean onPreOpen(@NotNull MenuPaginationContext<T> ctx, @NotNull MenuConfig openHandler) {
        return super.onPreOpen(ctx, openHandler);
    }
    
    void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        
        final MenuPaginationContext<T> ctx = new MenuPaginationContext<>(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) return;
        
        ctx.initialize();
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
}

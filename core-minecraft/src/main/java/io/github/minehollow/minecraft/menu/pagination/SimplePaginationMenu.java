package io.github.minehollow.minecraft.menu.pagination;

import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.Map;


public abstract class SimplePaginationMenu<T> extends SimpleMenu {
    public boolean onPreOpen(@NotNull MenuPaginationContext<T> ctx, @NotNull MenuConfig openHandler) {
        return super.onPreOpen(ctx, openHandler);
    }
    
    @Override
    protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        
        final MenuPaginationContext<T> ctx = new MenuPaginationContext<>(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) return;
        
        ctx.initialize();
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
}

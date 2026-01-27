package io.github.minehollow.minecraft.menu.pagination;

import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.Map;

public class DynamicPaginationMenu<T> extends SimpleMenu {
    public boolean onPreOpen(@NotNull DynamicPaginationContext<T> ctx, @NotNull MenuConfig openHandler) {
        return super.onPreOpen(ctx, openHandler);
    }
    
    @Override
    public void onExternalUpdate(@NotNull PlayerMenuContext ctx) {
        if (!(ctx instanceof DynamicPaginationContext<?> context)) return;
        context.refreshData();
    }
    
    @Override
    protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        
        final DynamicPaginationContext<T> ctx = new DynamicPaginationContext<>(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) { return; }
        
        ctx.initialize();
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
}

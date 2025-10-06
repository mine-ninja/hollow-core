package net.warcane.lugin.core.minecraft.menu.pagination;

import net.warcane.lugin.core.minecraft.menu.PlayerMenuContext;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
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

package io.github.minehollow.minecraft.menu;

import io.github.minehollow.minecraft.menu.config.MenuConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class SimpleMenu {
    protected final Map<UUID, PlayerMenuContext> playerContexts = new HashMap<>();
    protected final MenuConfig defaultConfig = new MenuConfig();


    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        return true;
    }

    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) { }

    protected void onClose(@NotNull PlayerMenuContext ctx, @NotNull InventoryCloseEvent event) { }

    protected void onTick(@NotNull MenuContext ctx) { }

    public void onExternalUpdate(@NotNull PlayerMenuContext ctx) { }
    
    protected void onError(@NotNull PlayerMenuContext ctx, @Nullable InventoryEvent event, @NotNull Throwable error) {
        // The default implementation does nothing.
    }
    
    public void closeForAllPlayers() {
        for (PlayerMenuContext value : playerContexts.values()) {
            value.close();
        }
    }
    
    protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        
        final PlayerMenuContext ctx = new PlayerMenuContext(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) { return; }
        
        ctx.initialize();
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
}

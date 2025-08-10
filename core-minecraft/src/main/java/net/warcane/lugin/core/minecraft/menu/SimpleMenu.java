package net.warcane.lugin.core.minecraft.menu;

import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SimpleMenu {

    protected final Map<UUID, PlayerMenuContext> playerContexts = new HashMap<>();
    protected final MenuConfig defaultConfig = new MenuConfig();


    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        return true;
    }

    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        // The default implementation does nothing.
    }

    protected void onClose(@NotNull PlayerMenuContext ctx, @NotNull InventoryCloseEvent event) {
        // The default implementation does nothing.
    }

    protected void onTick(@NotNull PlayerMenuContext ctx) {
        // The default implementation does nothing.
    }

    protected void onError(@NotNull PlayerMenuContext ctx, @Nullable InventoryEvent event, @NotNull Throwable error) {
        // The default implementation does nothing.
    }

    @NotNull
    public MenuConfig getDefaultConfig() {
        return defaultConfig;
    }

    public void closeForAllPlayers() {
        for (PlayerMenuContext value : playerContexts.values()) {
            value.close();
        }
    }

    void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        final var ctx = new PlayerMenuContext(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) return;

        ctx.initialize();
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
}

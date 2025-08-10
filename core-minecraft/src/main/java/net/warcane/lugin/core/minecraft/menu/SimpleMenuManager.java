package net.warcane.lugin.core.minecraft.menu;


import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.tick.AsyncServerTickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleMenuManager implements Listener {

    private final BukkitPlatform platform;
    private final Map<Class<? extends SimpleMenu>, SimpleMenu> menus = new LinkedHashMap<>();

    public SimpleMenuManager(@NotNull BukkitPlatform platform) {
        this.platform = platform;
        Bukkit.getPluginManager().registerEvents(this, platform.getPlugin());
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, platform.getPlugin());
    }

    public void register(@NotNull SimpleMenu menu) {
        menus.put(menu.getClass(), menu);
    }

    public void register(@NotNull SimpleMenu... menu) {
        for (SimpleMenu m : menu) {
            register(m);
        }
    }

    public void openToPlayer(@NotNull Player player, @NotNull Class<? extends SimpleMenu> menuClass) {
        openToPlayer(player, menuClass, new HashMap<>());
    }

    public void openToPlayer(@NotNull Player player, @NotNull Class<? extends SimpleMenu> menuClass, @NotNull Map<String, Object> initialData) {
        final var menu = menus.get(menuClass);
        if (menu == null) {
            throw new IllegalArgumentException("Menu class not registered: " + menuClass.getName());
        }

        menu.openToPlayer(this, player, initialData);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerClickMenu(InventoryClickEvent event) {
        final var inventory = event.getClickedInventory();
        if (inventory == null) return;

        final var context = getContextFromInventory(inventory);
        if (context == null) return;

        final var menu = context.getMenu();
        if (menu.defaultConfig.isGlobalClickCancelled()) {
            event.setCancelled(true);
        }

        try {
            final var contextItem = context.getItem(event.getRawSlot());
            if (contextItem != null) {
                contextItem.clickHandler().accept(event);
            }

            context.getMenuConfig().playClickSound(context.getPlayer());
            menu.onClick(context, event);
        } catch (Exception e) {
            menu.onError(context, event, e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCloseMenu(InventoryCloseEvent event) {
        final var context = getContextFromInventory(event.getInventory());
        if (context == null) return;

        final var menu = context.getMenu();
        try {
            context.getMenuConfig().playCloseSound(context.getPlayer());
            menu.playerContexts.remove(context.getPlayer().getUniqueId());

            menu.onClose(context, event);
        } catch (Exception e) {
            menu.onError(context, event, e);
        }
    }

    @EventHandler
    public void onTick(AsyncServerTickEvent event) {
        for (SimpleMenu menu : menus.values()) {
            for (PlayerMenuContext context : menu.playerContexts.values()) {
                if (!context.getMenuConfig().isTickUpdateEnabled()) continue;

                long updateIntervalMillis = context.getMenuConfig().getUpdateIntervalMillis();
                long elapsed = context.stopwatch.elapsedTimeInMillis();
                if (elapsed < updateIntervalMillis) continue;

                context.stopwatch.reset();
                menu.onTick(context);
            }
        }
    }


    private PlayerMenuContext getContextFromPlayer(@NotNull Player player) {
        final var inventory = player.getOpenInventory().getTopInventory();
        return getContextFromInventory(inventory);
    }

    private PlayerMenuContext getContextFromInventory(@NotNull Inventory inventory) {
        if (!(inventory.getHolder() instanceof PlayerMenuContext context)) {
            return null;
        }
        return context;
    }
}

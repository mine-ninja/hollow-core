package io.github.minehollow.minecraft.menu;

import io.github.minehollow.minecraft.BukkitPlatform;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MenuUtil {

    public static void openMenu(
      @NotNull Player player,
      @NotNull Class<? extends SimpleMenu> menuToOpen,
      @NotNull Map<String, Object> data
    ) {
        BukkitPlatform.getInstance()
          .getMenuManager()
          .openToPlayer(player, menuToOpen, data);
    }

    public static void openMenu(
      @NotNull Player player,
      @NotNull Class<? extends SimpleMenu> menuToOpen
    ) {
        BukkitPlatform.getInstance()
          .getMenuManager()
          .openToPlayer(player, menuToOpen);
    }

    public static void registerMenus(
      @NotNull SimpleMenu... menus
    ) {
        BukkitPlatform.getInstance()
          .getMenuManager()
          .register(menus);
    }
}

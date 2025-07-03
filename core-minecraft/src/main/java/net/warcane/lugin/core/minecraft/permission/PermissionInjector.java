package net.warcane.lugin.core.minecraft.permission;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for injecting permissions into a {@link Permissible} object, typically a {@link Player}.
 */
public interface PermissionInjector {

    void inject(@NotNull Player player, @NotNull Permissible permissible);
}

package net.warcane.lugin.core.minecraft.permission;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Representa um injetor de permissões para jogadores.
 */
public interface PermissionInjector {

    void injectPermissions(@NotNull Player player);
}

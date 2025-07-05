package net.warcane.lugin.core.minecraft.permission;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

// todo suporte a multiplas versões do jogo para injetar permissões.
@RequiredArgsConstructor
public class NmsPermissionInjector implements PermissionInjector {

    private final BukkitPlatform platform;

    @Override
    public void injectPermissions(@NotNull Player player) {

    }
}
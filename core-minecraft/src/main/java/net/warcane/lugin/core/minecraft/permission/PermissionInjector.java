package net.warcane.lugin.core.minecraft.permission;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.util.version.VersionChecker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Representa um injetor de permissões para jogadores.
 */
public interface PermissionInjector {

    /**
     * Cria um injetor de permissões baseado na plataforma atual.
     *
     * @param platform A plataforma Bukkit atual.
     * @return Um injetor de permissões adequado para a versão da plataforma.
     */
    static PermissionInjector fromCurrentPlatform(@NotNull BukkitPlatform platform) {
        if (VersionChecker.isLegacyVersion()) {
            return new LegacyPermissionInjector(platform);
        } else {
            return new ModernPermissionInjector(platform);
        }
    }

    void injectPermissions(@NotNull Player player);
}

package io.github.minehollow.minecraft.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.permission.legacy.LegacyPermissibleInjector;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// todo suporte a multiplas versões do jogo para injetar permissões.
@Slf4j
@RequiredArgsConstructor
public class LegacyPermissionInjector implements PermissionInjector {

    private final BukkitPlatform platform;

    @Override
    public void injectPermissions(@NotNull Player player) {
        final var localCachedAccount = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
        if (localCachedAccount == null) {
            throw new IllegalStateException("Player account not found for player: " + player.getName());
        }

        LegacyPermissibleInjector.injectPlayer(player, new PlayerAccountPermissible(
          player,
          localCachedAccount,
          platform.getGroupPermissionService()
        ));

        log.info("Injected permissions for player: {}", player.getName());
    }
}
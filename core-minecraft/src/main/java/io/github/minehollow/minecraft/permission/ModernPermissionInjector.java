package io.github.minehollow.minecraft.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.permission.modern.ModernPermissibleInjector;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class ModernPermissionInjector implements PermissionInjector {

    private final BukkitPlatform platform;

    @Override
    public void injectPermissions(@NotNull Player player) {
        final var cachedAccount = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
        if (cachedAccount == null) {
            throw new IllegalStateException("Player account not found for player: " + player.getName());
        }

        ModernPermissibleInjector.inject(player, new PlayerAccountPermissible(
          player,
          cachedAccount,
          platform.getGroupPermissionService()
        ));

        log.info("Injected permissions for player: {}", player.getName());
    }
}

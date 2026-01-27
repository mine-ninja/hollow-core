package io.github.minehollow.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.account.PlayerPermissionExpireEvent;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.permission.PlayerLosePermissionPacket;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class PlayerPermissionUpdatingListener implements Listener {

    private static final Stopwatch STOPWATCH = new Stopwatch();
    private static final int UPDATE_INTERVAL_SECONDS = 60; // Atualizar a cada 60 segundos

    private final BukkitPlatform platform;

    @EventHandler
    public void onTick(AsyncServerTickEvent event) {
        if (STOPWATCH.elapsedTimeInSeconds() < UPDATE_INTERVAL_SECONDS) {
            return;
        }

        STOPWATCH.reset();

        for (var account : platform.getPlayerAccountService().getCachedAccounts()) {
            updatePlayerPermissions(account);
        }
    }


    private void updatePlayerPermissions(@NotNull PlayerAccount account) {
        for (var playerPermission : account.permissions()) {
            if (playerPermission.isPermanent() || !playerPermission.isExpired()) continue;

            platform.getPlayerAccountService()
                .updatePlayerAccount(account.removePermissions(playerPermission.permission()))
                .whenComplete((updatedAccount, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        log.error("Failed to update player account {}: {}", account.uniqueId(), error.getMessage());
                        return;
                    }

                    final var expireEvent = new PlayerPermissionExpireEvent(account, playerPermission);
                    Bukkit.getPluginManager().callEvent(expireEvent);

                    final var playerLosePermissionPacket = new PlayerLosePermissionPacket(updatedAccount.uniqueId(), playerPermission.permission());
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, playerLosePermissionPacket);

                    log.info("Player permission expired for account {}: {}", account.uniqueId(), playerPermission.permission());
                });
        }
    }
}

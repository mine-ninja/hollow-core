package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.PlayerPermissionExpireEvent;
import net.warcane.lugin.core.minecraft.event.tick.AsyncServerTickEvent;
import net.warcane.lugin.core.minecraft.util.stopwatch.Stopwatch;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerLosePermissionPacket;
import net.warcane.lugin.core.player.account.PlayerAccount;
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

package io.github.minehollow.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.account.subscription.PlayerSubscriptionExpireEvent;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.permission.PlayerLoseGroupPacket;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.player.subscription.PlayerGroupSubscription;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class PlayerGroupUpdatingListener implements Listener {

    private static final Stopwatch STOPWATCH = new Stopwatch();
    private static final int UPDATE_INTERVAL_SECONDS = 60; // Atualizar a cada 60 segundos

    private final BukkitPlatform platform;

    @EventHandler
    public void onTick(AsyncServerTickEvent event) {
        if (STOPWATCH.elapsedTimeInSeconds() < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        STOPWATCH.reset();

        for (PlayerAccount account : platform.getPlayerAccountService().getCachedAccounts()) {
            updatePlayerSubscriptions(account);
        }
    }


    private void updatePlayerSubscriptions(@NotNull PlayerAccount account) {
        for (PlayerGroupSubscription subscription : account.subscriptions()) {
            if (subscription.isPermanent() || !subscription.isExpired()) continue;

            platform.getPlayerAccountService()
              .updatePlayerAccount(account.removeSubscription(subscription))
              .whenComplete((updatedAccount, error) -> {
                  if (error != null) {
                      error.printStackTrace();
                      log.error("Failed to update player account {}: {}", account.uniqueId(), error.getMessage());
                      return;
                  }

                  final var expireEvent = new PlayerSubscriptionExpireEvent(account, subscription);
                  Bukkit.getPluginManager().callEvent(expireEvent);

                  final var playerLoseGroupPacket = new PlayerLoseGroupPacket(updatedAccount.uniqueId(), subscription.group(), subscription.type());
                  platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, playerLoseGroupPacket);

                  log.info("Player subscription expired for account {}: {}", account.uniqueId(), subscription.group().getDisplayName());
              });
        }
    }
}

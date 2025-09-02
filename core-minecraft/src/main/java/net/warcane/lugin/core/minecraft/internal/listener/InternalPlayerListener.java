package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountUpdateEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.LocationUtil;
import net.warcane.lugin.core.minecraft.vanish.VanishManager;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectedToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import net.warcane.lugin.core.player.account.PlayerAccountService;
import net.warcane.lugin.core.player.account.PlayerAccountService.AccountUnloadOptions;
import net.warcane.lugin.core.player.fetcher.PlayerNameFetcher;
import net.warcane.lugin.core.player.fetcher.PlayerUuidFetcher;
import net.warcane.lugin.core.player.state.PlayerNetworkState;
import net.warcane.lugin.core.player.state.PlayerNetworkStateManager;
import net.warcane.lugin.core.player.teleport.PlayerJoinDataManager;
import net.warcane.lugin.core.player.wallet.Wallet;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static net.warcane.lugin.core.minecraft.task.Tasks.runAsync;
import static net.warcane.lugin.core.minecraft.task.Tasks.runAsyncLater;
import static net.warcane.lugin.core.player.account.PlayerAccount.createDefaultAccount;
import static net.warcane.lugin.core.player.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;
import static net.warcane.lugin.core.player.wallet.WalletService.LoadWalletOptions.withDefaultWallet;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {
    
    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";

    private final BukkitPlatform platform;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        try {
            final var uniqueId = event.getUniqueId();
            if (Property.get("ALLOWED_GROUPS") == null) {
                log.info("Server has no group restrictions, allowing all players to join.");
                return;
            }

            log.info("Player with UUID {} is attempting to join the server.", uniqueId);
            final var account = platform.getPlayerAccountService().loadPlayerAccount(uniqueId, new PlayerAccountService.AccountLoadOptions(null, false)).join();
            if (account == null) {
                log.error("Failed to load player account for UUID {} during pre-login.", uniqueId);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(platform.getDisallowJoinMessage()));
                return;
            }

            final var subscriptions = account.subscriptions();
            final boolean isAbleToJoin = subscriptions.stream().anyMatch(sub -> platform.isGroupAllowedToJoin(sub.group()));
            if (!isAbleToJoin) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(platform.getDisallowJoinMessage()));
                return;
            }

            final int currentPlayers = Bukkit.getOnlinePlayers().size();
            final int whitelistMaxPlayers = platform.getWhitelistService().getWhitelistPlayers();
            if (platform.getWhitelistService().isWhitelistEnabled() && currentPlayers >= whitelistMaxPlayers) {
                final boolean shouldBypass = subscriptions.stream().anyMatch(sub -> sub.group().isSpecialGroup());
                if (shouldBypass) {
                    log.info("Player with UUID {} is allowed to bypass the whitelist and join the full server.", uniqueId);
                    return;
                }

                log.info("Server is whitelisted and full ({} / {}), denying access to player with UUID {}.", currentPlayers, whitelistMaxPlayers, uniqueId);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("§cO Servidor está lotado no momento. Tente novamente mais tarde."));
                return;
            }


        } catch (Exception e) {
            e.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(FAILED_TO_LOAD_ERR_MSG));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var playerId = player.getUniqueId();
        final var name = player.getName();
        
        platform.getPlayerAccountService().loadPlayerAccount(playerId, withDefaultAccount(createDefaultAccount(playerId, name), true))
            .whenComplete((playerAccount, error) -> {
            if (error != null) {
                error.printStackTrace();
                log.error("Failed to load player account for {}: {}", player.getName(), error.getMessage(), error);
                this.syncKick(player);
                return;
            }

            try {
                final var categoryType = platform.getSubscriptionCategoryType();
                PlayerGroup group = playerAccount.getHighestSubscription(categoryType).group();
                if (Property.get("ALLOWED_GROUPS") != null && !platform.isGroupAllowedToJoin(group)) {
                    log.info("Player {} with UUID {} has group {} which is not allowed to join the server.", player.getName(), playerId, group.name());
                    this.syncKick(player, platform.getDisallowJoinMessage());
                    return;
                }

                // Só envia o pacote de connect caso realmente carregue as informações do jogador.
                // caso o contrario ele vai ser kickado (como mostra no código acima).
                final var packet = new PlayerConnectedToServerPacket(playerId, currentServerId);
                platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

                log.debug("Player account loaded for {}: {}", player.getName(), playerAccount);
                platform.getGameServerService().update(platform.getGameServer());
                platform.getPermissionInjector().injectPermissions(player);
                
                if (platform.getServerCategoryType() == ServerCategoryType.LOGIN) {
                    Tasks.runSync(() -> player.setGameMode(GameMode.ADVENTURE));
                }

                PlayerUuidFetcher.getInstance().cachePlayerUuid(name, playerId);
                PlayerNameFetcher.getInstance().setPlayerName(playerId, name);

                PlayerNetworkStateManager.getInstance().register(new PlayerNetworkState(
                  player.getUniqueId(),
                  player.getName(),
                  currentServerId,
                  platform.getServerCategoryType()
                ));


                if (!name.equals(playerAccount.playerName())) {
                    platform.getPlayerAccountService()
                      .updatePlayerAccount(playerAccount.withNewName(name))
                      .whenComplete((updatedAccount, updateError) -> {
                          if (updateError != null) {
                              log.error("Failed to update player account name for {}: {}", player.getName(), updateError.getMessage(), updateError);
                              this.syncKick(player);
                          } else {
                              log.info("Player account name updated for {}: {}", player.getName(), updatedAccount);
                          }
                      });
                }


                final var joinData = PlayerJoinDataManager.getInstance().getPlayerJoinData(playerId);
                if (joinData != null && currentServerId.equalsIgnoreCase(joinData.remoteServerLocation().targetServerId())) {
                    Tasks.runAsyncLater(() -> {
                        final var location = LocationUtil.transformLocation(joinData.remoteServerLocation());
                        player.teleportAsync(location);
                        PlayerJoinDataManager.getInstance().removeJoinData(playerId);
                    }, 1);
                }

                Tasks.runSync(() -> {
                    // TODO: favor não me crucificar isso é temporário

                    if (BukkitPlatform.getInstance().getVanishManager().isVanished(player)) {
                        BukkitPlatform.getInstance().getVanishManager().vanish(player);
                    }

                    for (Player target : Bukkit.getOnlinePlayers()) {
                        VanishManager vanishManager = BukkitPlatform.getInstance().getVanishManager();

                        if (vanishManager.isVanished(target) && !vanishManager.canSeeIfVanished(player, target)) {
                            player.hidePlayer(target);
                        }
                    }
                });

                Tasks.runAsyncLater(() -> {
                    Bukkit.getPluginManager().callEvent(new PlayerAccountLoadEvent(playerAccount));
                }, 1);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error while processing player join for {}: {}", player.getName(), e.getMessage(), e);
                this.syncKick(player);
                return;
            }
        });

        platform.getPlayerStatisticsService().loadPlayerAccount(playerId).whenComplete((playerStatistics, statisticsError) -> {
            if (statisticsError != null) {
                log.error("Failed to load player statistics for {}: {}", player.getName(), statisticsError.getMessage(), statisticsError);
                this.syncKick(player);
                return;
            }

            log.info("Player statistics loaded for {}: {}", player.getName(), playerStatistics);
        });

        platform.getWalletService().loadPlayerWallet(
          playerId,
          withDefaultWallet(Wallet.createDefaultWallet(playerId, name), true)
        ).whenComplete((playerWallet, walletError) -> {
            if (walletError != null) {
                log.error("Failed to load player wallet for {}: {}", player.getName(), walletError.getMessage(), walletError);
                this.syncKick(player);
                return;
            }

            log.info("Player wallet loaded for {}: {}", player.getName(), playerWallet);
        });

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        // Envia o pacote de desconexão do jogador para o servidor, mesmo que não tenha a conta atualizada.
        final var packet = new PlayerDisconnectedFromServerPacket(player.getUniqueId(), currentServerId);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);
        
        //        platform.getPlayerStatisticsService().unloadPlayerAccount(player.getUniqueId()).whenComplete((unloaded, error) -> {
        //            if (error != null) {
        //                log.error("Failed to unload player statistics for {}: {}", player.getName(), error.getMessage(), error);
        //            } else if (unloaded == null) {
        //                log.info("Player statistics not found for {} during unload", player.getName());
        //            } else {
        //                log.info("Player statistics unloaded for {}: {}", player.getName(), unloaded);
        //            }
        //        });
        
        runAsync(() -> {
            final var state = PlayerNetworkStateManager.getInstance().getPlayerState(player.getUniqueId());
            if (state != null) {
                PlayerNetworkStateManager.getInstance().unregister(state);
            }
        });

        final var unloadOptions = new AccountUnloadOptions(false, true);

        platform.getPlayerStatisticsService()
          .unloadPlayerAccount(player.getUniqueId())
          .whenComplete((unloaded, error) -> {
              if (error != null) {
                  log.error("Failed to unload player statistics for {}: {}", player.getName(), error.getMessage(), error);
              } else if (unloaded == null) {
                  log.info("Player statistics not found for {} during unload", player.getName());
              } else {
                  log.info("Player statistics unloaded for {}: {}", player.getName(), unloaded);
              }
          });

        platform.getPlayerAccountService()
          .unloadPlayerAccount(player.getUniqueId(), unloadOptions)
          .whenComplete((unloaded, error) -> {
            if (error != null) {
                log.error("Failed to unload player account for {}: {}", player.getName(), error.getMessage(), error);
            } else if (unloaded == null) {
                log.info("Player account not found for {} during unload", player.getName());
            } else {
                log.info("Player account unloaded for {}: {}", player.getName(), unloaded);
            }

            runAsyncLater(platform::updateServerInfo, 20);
        });
    }

    @EventHandler
    public void onGroupUpdate(PlayerAccountUpdateEvent event) {
        Player localPlayer = event.getLocalPlayer();
        if (localPlayer == null) return;
        
        platform.getPermissionInjector().injectPermissions(localPlayer);
    }
    
    private void syncKick(@NotNull Player player) {
        this.syncKick(player, FAILED_TO_LOAD_ERR_MSG);
    }

    private void syncKick(@NotNull Player player, @NotNull String reason) {
        Tasks.runSync(() -> {
            if (player.isOnline()) {
                player.kickPlayer(reason);
            }
        });
    }
}

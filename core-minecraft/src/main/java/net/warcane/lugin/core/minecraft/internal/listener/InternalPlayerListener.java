package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountUpdateEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.LocationUtil;
import net.warcane.lugin.core.minecraft.util.Tab;
import net.warcane.lugin.core.minecraft.util.nametag.NameTags;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectedToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.warcane.lugin.core.minecraft.task.Tasks.runAsync;
import static net.warcane.lugin.core.minecraft.task.Tasks.runAsyncLater;
import static net.warcane.lugin.core.player.account.PlayerAccount.createDefaultAccount;
import static net.warcane.lugin.core.player.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;
import static net.warcane.lugin.core.player.wallet.WalletService.LoadWalletOptions.withDefaultWallet;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final List<String> HEADER = List.of("§b§lLUGIN.COM.BR");
    private static final List<String> FOOTER = List.of("§aRanks, cosméticos e caixas em §c§lLUGIN.COM.BR");

    private static final Tab TAB = new Tab(HEADER, FOOTER);

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";

    private final BukkitPlatform platform;


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var playerId = player.getUniqueId();
        final var name = player.getName();

        platform.getPlayerAccountService().loadPlayerAccount(
          playerId,
          withDefaultAccount(createDefaultAccount(playerId, name), true)
        ).whenComplete((playerAccount, error) -> {
            if (error != null) {
                error.printStackTrace();
                log.error("Failed to load player account for {}: {}", player.getName(), error.getMessage(), error);
                this.syncKick(player);
                return;
            }

            try {
                final var categoryType = platform.getSubscriptionCategoryType();
                PlayerGroup group = playerAccount.getHighestSubscription(categoryType).group();
                final var priority = group.getPriorityValue();
                final var groupPrefix = group.getPrefix();


                // Só envia o pacote de connect caso realmente carregue as informações do jogador.
                // caso o contrario ele vai ser kickado (como mostra no código acima).
                final var packet = new PlayerConnectedToServerPacket(playerId, currentServerId);
                platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

                log.debug("Player account loaded for {}: {}", player.getName(), playerAccount);
                platform.getGameServerService().update(platform.getGameServer());
                platform.getPermissionInjector().injectPermissions(player);

                TAB.tick(player);
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


                Tasks.runAsyncLater(() -> {
                    final var loadTagsOnJoin = Property.getBoolean("LOAD_TAGS_ON_JOIN", true);
                    if (loadTagsOnJoin) {
                        NameTags.setNameTag(player, groupPrefix, "", priority, group.getNamedTextColor());
                        NameTags.updateAllTags();
                    }

                    Bukkit.getPluginManager().callEvent(new PlayerAccountLoadEvent(playerAccount));
                }, 1);

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error while processing player join for {}: {}", player.getName(), e.getMessage(), e);
                this.syncKick(player);
                return;
            }
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

        NameTags.removeNameTag(player);

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

        final var categoryType = platform.getSubscriptionCategoryType();
        PlayerGroup group = event.getPlayerAccount().getHighestSubscription(categoryType).group();
        final var priority = group.getPriorityValue();
        final var groupPrefix = group.getPrefix();

        final var loadTagsOnJoin = Property.get("LOAD_TAGS_ON_JOIN", "true").equalsIgnoreCase("true");
        if (loadTagsOnJoin) {
            NameTags.setNameTag(localPlayer, groupPrefix, "", priority, group.getNamedTextColor());
            NameTags.updateAllTags();
        }

        platform.getPermissionInjector().injectPermissions(localPlayer);
    }


    private void syncKick(@NotNull Player player) {
        Tasks.runSync(() -> {
            if (player.isOnline()) {
                player.kickPlayer(InternalPlayerListener.FAILED_TO_LOAD_ERR_MSG);
            }
        });
    }


}

package io.github.minehollow.minecraft.internal.listener;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.account.AsyncPlayerNickUpdateEvent;
import io.github.minehollow.minecraft.event.account.PlayerAccountLoadEvent;
import io.github.minehollow.minecraft.event.account.PlayerAccountUpdateEvent;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.LocationUtil;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.minecraft.vanish.VanishManager;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectedToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.player.account.PlayerAccountService;
import io.github.minehollow.sdk.player.account.PlayerAccountService.AccountUnloadOptions;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.player.teleport.PlayerJoinDataManager;
import io.github.minehollow.sdk.player.wallet.Wallet;
import io.github.minehollow.sdk.player.wallet.WalletService;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.property.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
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

import java.time.Instant;
import java.util.Set;

import static io.github.minehollow.minecraft.task.Tasks.runAsyncLater;
import static io.github.minehollow.sdk.player.account.PlayerAccount.createDefaultAccount;
import static io.github.minehollow.sdk.player.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;
import static io.github.minehollow.sdk.player.wallet.WalletService.LoadWalletOptions.withDefaultWallet;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";

    private static final Stopwatch TIMER = new Stopwatch();

    private final BukkitPlatform platform;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        final var uniqueId = event.getUniqueId();
        final var name = event.getName();

        try {
            log.info("Player with UUID {} is attempting to join the server.", uniqueId);

            var account = platform.getPlayerAccountService()
              .loadPlayerAccount(uniqueId, PlayerAccountService.AccountLoadOptions.withDefaultAccount(
                PlayerAccount.createDefaultAccount(uniqueId, name, null),
                true
              )).join();

            if (!name.equals(account.playerName())) {
                String oldName = account.playerName();
                try {
                    account = platform.getPlayerAccountService().updatePlayerAccount(account.withNewName(name)).join();
                } catch (Exception e) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("§cHouve um erro ao atualizar o seu nome de jogador. Tente novamente mais tarde."));
                    e.printStackTrace();
                    return;
                }

                AsyncPlayerNickUpdateEvent nickUpdateEvent = new AsyncPlayerNickUpdateEvent(account, oldName, name);
                nickUpdateEvent.callEvent();
                if (nickUpdateEvent.isCancelled()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("§cHouve um erro ao atualizar o seu nome de jogador.\n").append(nickUpdateEvent.getCanceledMessage()));
                    return;
                }
            }

            if (platform.getServerCategoryType() != ServerCategoryType.LOGIN) {
                final var wallet = platform.getWalletService()
                  .loadPlayerWallet(event.getUniqueId(),
                    withDefaultWallet(Wallet.createDefaultWallet(event.getUniqueId(), event.getName()), true)
                  ).join();

                if (wallet == null) {
                    log.error("Failed to load wallet for UUID {} during pre-login.", uniqueId);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(FAILED_TO_LOAD_ERR_MSG));
                } else {
                    log.info("Wallet loaded for player UUID {}: {}", uniqueId, wallet);
                }
            }

            if (platform.isGroupAllowedToJoin(PlayerGroup.DEFAULT) && !this.isServerFull()) {
                return;
            }

            final var subscriptions = account.subscriptions();
            final var highestGroup = account.getHighestSubscription(platform.getSubscriptionCategoryType()).group();
            if (this.isServerFull() && highestGroup == PlayerGroup.DEFAULT) {
                log.info("Player with UUID {} is trying to join a full server and has no VIP group.", uniqueId);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cO servidor está cheio no momento. Apenas jogadores VIP podem entrar em servidores cheios. Considere adquirir um VIP em nosso site!");
                return;
            }


            final boolean isAbleToJoin = subscriptions.stream().anyMatch(sub -> platform.isGroupAllowedToJoin(sub.group()));
            if (!isAbleToJoin) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(platform.getDisallowJoinMessage()));
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

        // TODO - Buscar uma forma de atualizar a skin sempre q o jogador entrar.
        String skin = null;
        Set<ProfileProperty> properties = player.getPlayerProfile().getProperties();
        for (ProfileProperty property : properties) {
            if (property.getName().equals("textures")) {
                skin = property.getValue();
                break;
            }
        }

        platform.getPlayerAccountService()
          .loadPlayerAccount(playerId,
            withDefaultAccount(createDefaultAccount(playerId, name, skin), true)
          )
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

                  final var joinData = PlayerJoinDataManager.getInstance().getPlayerJoinData(playerId);
                  if (joinData != null && currentServerId.equalsIgnoreCase(joinData.remoteServerLocation().targetServerId())) {
                      runAsyncLater(() -> {
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

                          if (vanishManager.isVanished(target) && !vanishManager.canSeeIfVanished(player)) {
                              player.hidePlayer(target);
                          }
                      }
                  });


                  runAsyncLater(() -> {
                      Bukkit.getPluginManager().callEvent(new PlayerAccountLoadEvent(playerAccount));
                  }, 1);

              } catch (Exception e) {
                  e.printStackTrace();
                  log.error("Error while processing player join for {}: {}", player.getName(), e.getMessage(), e);
                  this.syncKick(player);
                  return;
              }
          });


//        platform.getPlayerDiscordService()
//          .loadPlayerDiscord(playerId)
//          .whenComplete((playerDiscord, throwable) -> {
//              if (throwable != null) {
//                  log.error("Failed to load player discord for {}: {}", player.getName(), throwable.getMessage(), throwable);
//                  return;
//              }
//
//              if (playerDiscord == null) {
//                  log.debug("Player discord not found for {} during load", player.getName());
//                  return;
//              }
//
//              log.debug("Player discord loaded for {}: {}", player.getName(), playerDiscord);
//          });
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var walletService = platform.getWalletService();
        final var wallet = walletService.getCachedWallet(player.getUniqueId());
        if (wallet != null) {
            walletService.unloadWallet(wallet, new WalletService.UnloadWalletOptions(true))
              .whenComplete((unloadedWallet, walletError) -> {
                  if (walletError != null) {
                      log.error("Failed to unload wallet for {}: {}", player.getName(), walletError.getMessage(), walletError);
                  } else if (unloadedWallet == null) {
                      log.info("Wallet not found for {} during unload", player.getName());
                  } else {
                      log.info("Wallet unloaded for {}: {}", player.getName(), unloadedWallet);
                  }
              });
        } else {
            log.warn("Wallet not found in cache for player: {}", player.getUniqueId());
        }

        // Envia o pacote de desconexão do jogador para o servidor, mesmo que não tenha a conta atualizada.
        final var packet = new PlayerDisconnectedFromServerPacket(player.getUniqueId(), currentServerId);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

        final var state = PlayerNetworkStateManager.getInstance().getPlayerState(player.getUniqueId());
        if (state != null) {
            PlayerNetworkStateManager.getInstance().unregister(state);
        }

        final var accountService = platform.getPlayerAccountService();
        accountService.getPlayerAccount(player.getUniqueId())
          .whenCompleteAsync((account, throwable) -> {
              if (account == null || throwable != null) {
                  if (throwable != null) {
                      log.error("Failed to get player account for {} during quit: {}", player.getName(), throwable.getMessage(), throwable);
                  } else {
                      log.debug("Player account not found for {} during quit", player.getName());
                  }
                  return;
              }

              accountService.updatePlayerAccount(account.withLastLogin(Instant.now()))
                .thenCompose(acc -> accountService.unloadPlayerAccount(acc.uniqueId(), new AccountUnloadOptions(false, true)))
                .whenComplete((unloaded, error) -> {
                    if (error != null) {
                        log.error("Failed to unload player account for {}: {}", player.getName(), error.getMessage(), error);
                    } else if (unloaded == null) {
                        log.info("Player account not found for {} during unload", player.getName());
                    } else {
                        log.debug("Player account unloaded for {}: {}", player.getName(), unloaded);
                    }

                    runAsyncLater(platform::updateServerInfo, 20);
                });
          }, Tasks::runAsync);

        platform.getPlayerDiscordService().unloadPlayerDiscord(player.getUniqueId());

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

    public boolean isServerFull() {
        final int maxSlots = this.getMaxServerSlots();
        if (maxSlots <= 0) return false;

        final int onlinePlayers = Bukkit.getOnlinePlayers().size();
        return onlinePlayers >= maxSlots - 1;
    }

    private int getMaxServerSlots() {
        final var fromProperty = Property.get("MAX_PLAYERS");
        if (fromProperty == null) return -1;

        try {
            return Integer.parseInt(fromProperty);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

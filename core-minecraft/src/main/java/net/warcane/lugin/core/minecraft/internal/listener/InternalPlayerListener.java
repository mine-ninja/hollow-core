package net.warcane.lugin.core.minecraft.internal.listener;

import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.AsyncPlayerNickUpdateEvent;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountUpdateEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.LocationUtil;
import net.warcane.lugin.core.minecraft.util.PlayerUtil;
import net.warcane.lugin.core.minecraft.util.SkinUtils;
import net.warcane.lugin.core.minecraft.util.version.VersionChecker;
import net.warcane.lugin.core.minecraft.vanish.VanishManager;
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
import net.warcane.lugin.core.player.wallet.WalletService;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.util.Set;

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
        final var uniqueId = event.getUniqueId();
        final var name = event.getName();

        try {
            log.info("Player with UUID {} is attempting to join the server.", uniqueId);

            var account = platform.getPlayerAccountService().getPlayerAccount(uniqueId).join();
            if (account == null) {
                log.error("Failed to load player account for UUID {} during pre-login.", uniqueId);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(platform.getDisallowJoinMessage()));
                return;
            }

            PlayerUuidFetcher.getInstance().cachePlayerUuid(name, uniqueId);
            PlayerNameFetcher.getInstance().setPlayerName(uniqueId, name);

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

            // carrega a carteira do cara sempre direto no login.
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

            // Matheus: Por enquanto da pra fazer assim... (Precisa de uma logica melhor depois)
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
        
        // TODO - Buscar uma forma de atualizar a skin sempre q o jogador entrar.
        String skin = null;
        Set<ProfileProperty> properties = player.getPlayerProfile().getProperties();
        for (ProfileProperty property : properties) {
            if (property.getName().equals("textures")) {
                skin = property.getValue();
                break;
            }
        }
        
        platform.getPlayerAccountService().loadPlayerAccount(playerId, withDefaultAccount(createDefaultAccount(playerId, name, skin), true))
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

                    PlayerNetworkStateManager.getInstance().register(new PlayerNetworkState(
                        player.getUniqueId(),
                        player.getName(),
                        currentServerId,
                        platform.getServerCategoryType(),
                        platform.getServerSubCategoryType()
                    ));

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

                    runAsyncLater(() -> Bukkit.getPluginManager().callEvent(new PlayerAccountLoadEvent(playerAccount)), 1);

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error while processing player join for {}: {}", player.getName(), e.getMessage(), e);
                    this.syncKick(player);
                    return;
                }
            });

        platform.getPlayerDiscordService().loadPlayerDiscord(playerId).whenComplete((playerDiscord, throwable) -> {
            if (throwable != null) {
                log.error("Failed to load player discord for {}: {}", player.getName(), throwable.getMessage(), throwable);
                return;
            }

            if (playerDiscord == null) {
                log.debug("Player discord not found for {} during load", player.getName());
                return;
            }

            log.debug("Player discord loaded for {}: {}", player.getName(), playerDiscord);
        });

        if (VersionChecker.isLegacyVersion()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitPlatform.getInstance().getPlugin(), () -> {
                if (PlayerUtil.isCracked(player)) {
                    SkinUtils.setPlayerSkin(player, "ewogICJ0aW1lc3RhbXAiIDogMTc1NzY1MTM5MTQ0NywKICAicHJvZmlsZUlkIiA6ICJmZDFhYjdjNTYwMTM0YzQ1YTA2YWZjZWY1OGViZDVkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDbGF1MTA3IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2ZlZjlhM2JkODY0ZmMxNmJlMzM0YzFmZDVjZmUwZDIxNTE2NTY2OTkxMGExYjU1YTQ5Yjg5NTk0ZTBjZjVhM2EiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==", "dDGv364IyP5jBYPgSyqZ0GkV0Lm3X019f3zF141ha3NJWO8PhjMwzQAArAqsGyTciCXzFHvLkdM6NsO/VZDv/Q8XFczMYSFokcxI3dpqKrwBGTrELM5WGqvJZBltmPovPUzexu0dUqvjHffRQhYCY82lbFYU24SugXbgQtIRqdRww/fN05Av3a6hLMm613ZEky1qs6WBsVJSJ7YsBhCVN42x69vRoB5w3bONdq6bw+B3VlqdKvT2JbVX17oBvAC/wGAlH5kF5K1+t4XKGkxMT7qd4g+e3Cx7Ze/681yY/oIeYKMF4V/KBA+A/m+cNqS2IPLhdlzBBKN5CLliF89s6LqtnGeRbBDMO7Gqm4g++DaOCC+D3HF/5YmE8DSyhV6rZO6e/N7smH93OZn4fvAwsbYSIb88Fpyn7UpZ7F751hJQJ9k1Kw8RYEdIk85Sw8f69D1ZTjd9ddJHV9tfgATJPQgJyjFHI5qNptYbbV/lFeCWGazg8XPoJe5Zbeja4Gi0veNM7LFH+vJUSpUQVKI9/4mQEUpYpFG5mZgHIEb+zqo0QWM5nUGYXu9PTt5KFxOGLsySzmTRg6ZjU82w+OiUShondvYIfKFRcozy0mFv/FQFosfx9fxm7dy1RfdwxhjPgstlnzuJBuXLZjbduSdAE6TLJVfKVKXUbfdM5oIe6xE=");
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var wallet = platform.getWalletService().getCachedWalletOrThrow(player.getUniqueId());
        platform.getWalletService()
            .unloadWallet(wallet, new WalletService.UnloadWalletOptions(true))
            .whenComplete((unloadedWallet, walletError) -> {
                if (walletError != null) {
                    log.error("Failed to unload wallet for {}: {}", player.getName(), walletError.getMessage(), walletError);
                } else if (unloadedWallet == null) {
                    log.info("Wallet not found for {} during unload", player.getName());
                } else {
                    log.info("Wallet unloaded for {}: {}", player.getName(), unloadedWallet);
                }
            });

        // Envia o pacote de desconexão do jogador para o servidor, mesmo que não tenha a conta atualizada.
        final var packet = new PlayerDisconnectedFromServerPacket(player.getUniqueId(), currentServerId);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

        runAsync(() -> {
            final var state = PlayerNetworkStateManager.getInstance().getPlayerState(player.getUniqueId());
            if (state != null) {
                PlayerNetworkStateManager.getInstance().unregister(state);
            }
        });
        
        final var accountService = platform.getPlayerAccountService();
        accountService.getPlayerAccount(player.getUniqueId())
            .whenCompleteAsync((account, throwable) -> {
                if (account == null || throwable != null) {
                    if (throwable != null) {
                        log.error("Failed to get player account for {} during quit: {}", player.getName(), throwable.getMessage(), throwable);
                    }
                    else {
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

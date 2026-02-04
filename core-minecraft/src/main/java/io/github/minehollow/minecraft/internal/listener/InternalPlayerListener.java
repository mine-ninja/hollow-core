package io.github.minehollow.minecraft.internal.listener;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.account.PlayerAccountLoadEvent;
import io.github.minehollow.minecraft.event.account.PlayerAccountUpdateEvent;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.LocationUtil;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectedToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import io.github.minehollow.sdk.player.account.PlayerAccountService.AccountUnloadOptions;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.player.teleport.PlayerJoinDataManager;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Set;

import static io.github.minehollow.sdk.player.account.PlayerAccount.createDefaultAccount;
import static io.github.minehollow.sdk.player.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;
import static io.github.minehollow.sdk.player.wallet.Wallet.createDefaultWallet;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";
    private static final Component FAILED_TO_LOAD_COMPONENT = Component.text(FAILED_TO_LOAD_ERR_MSG);
    private static final Component NAME_UPDATE_ERROR_COMPONENT = Component.text("§cHouve um erro ao atualizar o seu nome de jogador. Tente novamente mais tarde.");

    private static final Stopwatch TIMER = new Stopwatch();

    private final BukkitPlatform platform;

    private volatile String allowedGroupsProperty;
    private volatile Integer maxPlayersCache;
    private volatile long lastPropertyCheck;
    private static final long PROPERTY_CACHE_MS = 5000;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        if (platform.getServerCategoryType() == ServerCategoryType.LOGIN) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        final var currentServerId = platform.getId();
        final var playerId = player.getUniqueId();
        final var name = player.getName();
        final var skin = extractSkin(player);

        Thread.startVirtualThread(() -> {
            try {
                final var wallet = platform.getWalletService().loadWallet(
                  playerId,
                  createDefaultWallet(playerId, name),
                  true
                );


                if (wallet != null) {
                    platform.getWalletService().updateWallet(wallet);
                }

                log.debug("Wallet loaded for player {}: {}", player.getName(), wallet);
            } catch (Exception e) {
                log.error("Failed to load wallet for player {}: {}", player.getName(), e.getMessage(), e);
                this.syncKick(player, FAILED_TO_LOAD_ERR_MSG);
                return;
            }


            try {
                final var playerAccount = platform.getPlayerAccountService()
                  .loadPlayerAccount(playerId, withDefaultAccount(createDefaultAccount(playerId, name, skin), true))
                  .join();

                final var categoryType = platform.getSubscriptionCategoryType();
                PlayerGroup group = playerAccount.getHighestSubscription(categoryType).group();
                if (getCachedAllowedGroups() != null && !platform.isGroupAllowedToJoin(group)) {
                    log.debug("Player {} with UUID {} has group {} which is not allowed to join the server.", player.getName(), playerId, group.name());
                    this.syncKick(player, platform.getDisallowJoinMessage());
                    return;
                }

                final var packet = new PlayerConnectedToServerPacket(playerId, currentServerId);
                platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

                log.debug("Player account loaded for {}: {}", player.getName(), playerAccount);
                platform.getGameServerService().update(platform.getGameServer());
                platform.getPermissionInjector().injectPermissions(player);

                final var joinData = PlayerJoinDataManager.getInstance().getPlayerJoinData(playerId);
                if (joinData != null && currentServerId.equalsIgnoreCase(joinData.remoteServerLocation().targetServerId())) {
                    final var location = LocationUtil.transformLocation(joinData.remoteServerLocation());
                    player.teleportAsync(location);
                    PlayerJoinDataManager.getInstance().removeJoinData(playerId);
                }

                Thread.sleep(50);
                Bukkit.getPluginManager().callEvent(new PlayerAccountLoadEvent(playerAccount));
            } catch (Exception error) {
                log.error("Error during player join processing for {}: {}", player.getName(), error.getMessage(), error);
                this.syncKick(player, FAILED_TO_LOAD_ERR_MSG);
            }
        });


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();
        final var playerId = player.getUniqueId();
        final var accountService = platform.getPlayerAccountService();
        Thread.startVirtualThread(() -> {

            final var packet = new PlayerDisconnectedFromServerPacket(playerId, currentServerId);
            platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

            final var state = PlayerNetworkStateManager.getInstance().getPlayerState(playerId);
            if (state != null) {
                PlayerNetworkStateManager.getInstance().unregister(state);
            }

            final var walletService = platform.getWalletService();
            final var wallet = walletService.getCachedWallet(playerId);
            if (wallet != null) {
                walletService.clearCachedWallet(playerId);
                try {
                    walletService.updateWallet(wallet);
                    log.debug("Wallet saved for player {}: {}", player.getName(), wallet);
                } catch (Exception e) {
                    log.error("Failed to save wallet for player {}: {}", player.getName(), e.getMessage(), e);
                }
            } else {
                log.warn("Wallet not found in cache for player: {}", playerId);
            }


            try {
                final var account = accountService.getPlayerAccount(playerId).join();
                if (account == null) {
                    log.warn("Player account not found for {} during quit", player.getName());
                    return;
                }

                final var updatedAccount = accountService.updatePlayerAccount(account.withLastLogin(Instant.now())).join();
                log.debug("Player account updated for {}: {}", player.getName(), updatedAccount);

                accountService.unloadPlayerAccount(updatedAccount.uniqueId(), new AccountUnloadOptions(true, false)).join();

                Tasks.runAsyncLater(platform::updateServerInfo, 20);
            } catch (Exception e) {
                log.error("Failed to get player account for {} during quit: {}", player.getName(), e.getMessage(), e);
            }

        });
    }

    @EventHandler
    public void onGroupUpdate(PlayerAccountUpdateEvent event) {
        Player localPlayer = event.getLocalPlayer();
        if (localPlayer == null) return;

        platform.getPermissionInjector().injectPermissions(localPlayer);
    }

    private String extractSkin(Player player) {
        Set<ProfileProperty> properties = player.getPlayerProfile().getProperties();
        for (ProfileProperty property : properties) {
            if ("textures".equals(property.getName())) {
                return property.getValue();
            }
        }
        return null;
    }

    private String getCachedAllowedGroups() {
        long now = System.currentTimeMillis();
        if (allowedGroupsProperty == null || now - lastPropertyCheck > PROPERTY_CACHE_MS) {
            allowedGroupsProperty = Property.get("ALLOWED_GROUPS");
            lastPropertyCheck = now;
        }
        return allowedGroupsProperty;
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
        long now = System.currentTimeMillis();
        if (maxPlayersCache == null || now - lastPropertyCheck > PROPERTY_CACHE_MS) {
            final var fromProperty = Property.get("MAX_PLAYERS");
            if (fromProperty == null) {
                maxPlayersCache = -1;
            } else {
                try {
                    maxPlayersCache = Integer.parseInt(fromProperty);
                } catch (NumberFormatException e) {
                    maxPlayersCache = -1;
                }
            }
        }
        return maxPlayersCache;
    }
}
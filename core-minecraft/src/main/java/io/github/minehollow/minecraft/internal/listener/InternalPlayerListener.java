package io.github.minehollow.minecraft.internal.listener;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.LocationUtil;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectedToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.player.teleport.PlayerJoinDataManager;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.property.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static io.github.minehollow.sdk.player.wallet.Wallet.createDefaultWallet;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";
    private static final long PROPERTY_CACHE_MS = 5000;

    // Executor que utiliza Virtual Threads (Per-Task)
    private final ExecutorService vThreadExecutor = BukkitPlatform.getInstance().getExecutorService();
    private final BukkitPlatform platform;

    private volatile String allowedGroupsProperty;
    private volatile long lastGroupsCheck;

    private volatile Integer maxPlayersCache;
    private volatile long lastSlotsCheck;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        if (platform.getServerCategoryType() == ServerCategoryType.LOGIN) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        final var currentServerId = platform.getId();
        final var playerId = player.getUniqueId();
        final var name = player.getName();

        vThreadExecutor.submit(() -> {
            try {
                platform.getNetworkClient().sendNetworkPacket(
                    NetworkChannel.PLAYER_CONNECTION,
                    new PlayerConnectedToServerPacket(playerId, currentServerId)
                );

                final var wallet = platform.getWalletService().loadWallet(
                    playerId,
                    createDefaultWallet(playerId, name),
                    true
                );

                if (wallet != null) {
                    platform.getWalletService().updateWallet(wallet);
                }


                platform.getGameServerService().update(platform.getGameServer());

                final var joinData = PlayerJoinDataManager.getInstance().getPlayerJoinData(playerId);
                if (joinData != null && currentServerId.equalsIgnoreCase(joinData.remoteServerLocation().targetServerId())) {
                    final var location = LocationUtil.transformLocation(joinData.remoteServerLocation());
                    player.teleportAsync(location)
                        .thenRun(() -> PlayerJoinDataManager.getInstance().removeJoinData(playerId));
                }

            } catch (Exception e) {
                log.error("Erro no processamento de entrada de {}: {}", name, e.getMessage());
                this.syncKick(player, FAILED_TO_LOAD_ERR_MSG);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var playerId = player.getUniqueId();
        final var name = player.getName();
        final var currentServerId = platform.getId();

        vThreadExecutor.submit(() -> {
            try {
                platform.getNetworkClient().sendNetworkPacket(
                    NetworkChannel.PLAYER_CONNECTION,
                    new PlayerDisconnectedFromServerPacket(playerId, currentServerId)
                );

                final var state = PlayerNetworkStateManager.getInstance().getPlayerState(playerId);
                if (state != null) {
                    PlayerNetworkStateManager.getInstance().unregister(state);
                }

                final var walletService = platform.getWalletService();
                final var wallet = walletService.getCachedWallet(playerId);
                if (wallet != null) {
                    walletService.clearCachedWallet(playerId);
                    walletService.updateWallet(wallet);
                }
            } catch (Exception e) {
                log.error("Erro ao processar saída de {}: {}", name, e.getMessage());
            }
        });
    }

    private String extractSkin(Player player) {
        for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
            if ("textures".equals(property.getName())) {
                return property.getValue();
            }
        }
        return null;
    }

    public String getCachedAllowedGroups() {
        long now = System.currentTimeMillis();
        if (allowedGroupsProperty == null || now - lastGroupsCheck > PROPERTY_CACHE_MS) {
            allowedGroupsProperty = Property.get("ALLOWED_GROUPS");
            lastGroupsCheck = now;
        }
        return allowedGroupsProperty;
    }

    public boolean isServerFull() {
        final int maxSlots = this.getMaxServerSlots();
        if (maxSlots <= 0) return false;

        return Bukkit.getOnlinePlayers().size() >= maxSlots;
    }

    private int getMaxServerSlots() {
        long now = System.currentTimeMillis();
        if (maxPlayersCache == null || now - lastSlotsCheck > PROPERTY_CACHE_MS) {
            final var fromProperty = Property.get("MAX_PLAYERS");
            try {
                maxPlayersCache = (fromProperty != null) ? Integer.parseInt(fromProperty) : -1;
            } catch (NumberFormatException e) {
                maxPlayersCache = -1;
            }
            lastSlotsCheck = now;
        }
        return maxPlayersCache;
    }

    private void syncKick(@NotNull Player player, @NotNull String reason) {
        Tasks.runSync(() -> {
            if (player.isOnline()) {
                player.kick(LegacyComponentSerializer.legacySection().deserialize(reason));
            }
        });
    }
}

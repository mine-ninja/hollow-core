package io.github.minehollow.sdk;

import net.kyori.adventure.text.Component;
import io.github.minehollow.sdk.group.GroupPermissionService;
import io.github.minehollow.sdk.network.NetworkClient;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.SendMessageToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendModernMessageToPlayerPacket;
import io.github.minehollow.sdk.player.account.PlayerAccountService;
import io.github.minehollow.sdk.player.discord.PlayerDiscordService;
import io.github.minehollow.sdk.player.wallet.WalletService;
import io.github.minehollow.sdk.server.GameServerService;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Representa uma plataforma na rede.
 */
public interface Platform {
    ThreadLocalRandom SHARED_RANDOM = ThreadLocalRandom.current();
    
    void init(NetworkChannel... channels);
    
    default void init() {
        init(NetworkChannel.values());
    }
    
    void close();
    
    String getId();
    
    NetworkClient getNetworkClient();
    
    GameServerService getGameServerService();
    
    PlayerAccountService getPlayerAccountService();
    
    GroupPermissionService getGroupPermissionService();
    
    WalletService getWalletService();
    
    ExecutorService getExecutorService();

    PlayerDiscordService getPlayerDiscordService();
    
    default void sendMessageToPlayer(@NotNull UUID playerId, @NotNull String message) {
        getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new SendMessageToPlayerPacket(playerId, message));
    }
    
    default void sendMessageToPlayer(@NotNull UUID playerId, @NotNull Component component) {
        final var packet = SendModernMessageToPlayerPacket.create(playerId, component);
        getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, packet);
    }
    
    default void sendMessageToPlayer(@NotNull UUID playerId, @NotNull String message, String key) {
        getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new SendMessageToPlayerPacket(playerId, message, key));
    }
    
    default void sendMessageToPlayer(@NotNull UUID playerId, @NotNull Component component, String key) {
        final var packet = SendModernMessageToPlayerPacket.create(playerId, component, key);
        getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, packet);
    }
}

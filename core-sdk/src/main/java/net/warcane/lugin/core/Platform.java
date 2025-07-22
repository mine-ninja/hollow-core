package net.warcane.lugin.core;

import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.SendMessageToPlayerPacket;
import net.warcane.lugin.core.player.account.PlayerAccountService;
import net.warcane.lugin.core.player.wallet.WalletService;
import net.warcane.lugin.core.server.GameServerService;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Representa uma plataforma na rede.
 */
public interface Platform {

    ThreadLocalRandom SHARED_RANDOM = ThreadLocalRandom.current();

    void init(NetworkChannel... channels);

    default void init(){
        init(NetworkChannel.values());
    }

    void close();

    String getId();

    NetworkClient getNetworkClient();

    GameServerService getGameServerService();

    PlayerAccountService getPlayerAccountService();

    GroupPermissionService getGroupPermissionService();

    WalletService getWalletService();

    default void sendMessageToPlayer(@NotNull UUID playerId, @NotNull String message) {
        getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new SendMessageToPlayerPacket(playerId , message));
    }
}

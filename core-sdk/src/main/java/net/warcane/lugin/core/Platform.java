package net.warcane.lugin.core;

import net.warcane.lugin.core.account.PlayerAccountService;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.server.GameServerService;

/**
 * Representa uma plataforma na rede.
 */
public interface Platform {

    void init(NetworkChannel... channels);

    default void init(){
        init(NetworkChannel.values());
    }

    void close();

    String getId();

    NetworkClient getNetworkClient();

    GameServerService getGameServerService();

    PlayerAccountService getPlayerAccountService();
}

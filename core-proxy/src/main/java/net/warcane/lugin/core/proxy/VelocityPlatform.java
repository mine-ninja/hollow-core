package net.warcane.lugin.core.proxy;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.ProxyPlatform;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.proxy.listener.*;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class VelocityPlatform extends AbstractPlatform implements ProxyPlatform {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final ProxyServer proxyServer;

    public VelocityPlatform(@NotNull ProxyServer proxyServer) {
        super(HostAddress.fromInetSocketAddress(proxyServer.getBoundAddress()));
        this.proxyServer = proxyServer;
    }

    @Override
    public void init(NetworkChannel... channels) {
        networkClient.subscribeToChannels(channels);
        networkClient.registerPacketListener(ServerRegisterPacket.class, new ServerRegisterPacketListener(this));
        networkClient.registerPacketListener(PlayerTeleportToLocationPacket.class, new PlayerTeleportListener(this));
        networkClient.registerPacketListener(ServerUnregisterPacket.class, new ServerUnregisterPacketListener(this));
        networkClient.registerPacketListener(PlayerDirectPlayGameCategoryPacket.class, new PlayerDirectPlayGameCategoryListener(this));
        networkClient.registerPacketListener(PlayerConnectToServerPacket.class, new PlayerConnectToServerListener(this));

        int serverCount = 0;
        for (GameServer gameServer : gameServerService.queryAllServersInNetwork()) {
            this.registerServer(gameServer.serverId(), gameServer.hostAddress());
            serverCount++;
        }

        log.info("Registered {} servers in the proxy platform.", serverCount);
    }

    @Override
    public void close() {

    }

    @Override
    public void registerServer(@NotNull String serverId, @NotNull HostAddress address) {



        proxyServer.registerServer(new ServerInfo(serverId, address.asInetAddress()));
        log.info("Registered server: {} at address: {}", serverId, address);
    }

    @Override
    public void unregisterServer(@NotNull String serverId) {
        final var query = proxyServer.getServer(serverId);
        if (query.isEmpty()) {
            log.info("Server with ID {} is not registered in the proxy, skipping unregistration.", serverId);
            return;
        }

        final var serverInfo = query.get();
        proxyServer.unregisterServer(serverInfo.getServerInfo());
        gameServerService.unregister(serverId);
        log.info("Unregistered server: {}", serverId);
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    GameServer getRandomLobby() {
        final var allLobbyServers = gameServerService.queryAllServersInNetwork()
          .stream()
          .filter(server -> server.categoryType() == ServerCategoryType.LOBBY)
          .toList();

        if (allLobbyServers.isEmpty()) {
            return null; // No lobby servers available
        }

        return allLobbyServers.get(RANDOM.nextInt(allLobbyServers.size()));
    }
}

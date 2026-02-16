package io.github.minehollow.proxy;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.github.minehollow.sdk.AbstractPlatform;
import io.github.minehollow.sdk.ProxyPlatform;
import io.github.minehollow.proxy.listener.*;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToSubCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerRegisterPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerUnregisterPacket;
import io.github.minehollow.sdk.network.packet.impl.staff.GoCommandPacket;
import io.github.minehollow.proxy.listener.connection.ConnectionHandshakePacketListener;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.address.HostAddress;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
        log.info("Initializing VelocityPlatform...");

        networkClient.subscribeToChannels(channels);

        // Registrar listeners de pacotes
        networkClient.registerPacketListener(ServerRegisterPacket.class, new ServerRegisterPacketListener(this));
        networkClient.registerPacketListener(PlayerTeleportToLocationPacket.class, new PlayerTeleportListener(this));
        networkClient.registerPacketListener(ServerUnregisterPacket.class, new ServerUnregisterPacketListener(this));
        networkClient.registerPacketListener(PlayerDirectPlayGameCategoryPacket.class, new PlayerDirectPlayGameCategoryListener(this));
        networkClient.registerPacketListener(GoCommandPacket.class, new GoCommandPacketListener(this));
        networkClient.registerPacketListener(PlayerConnectToServerPacket.class, new PlayerConnectToServerListener(this));
        networkClient.registerPacketListener(PlayerConnectToSubCategoryPacket.class, new PlayerConnectToSubCategoryListener(this));
        networkClient.registerPacketListener(ConnectionHandshakePacket.class, new ConnectionHandshakePacketListener(proxyServer));

        log.info("Packet listeners registered successfully");

        // Registrar todos os servidores da network no Velocity
        int serverCount = 0;
        int lobbyCount = 0;

        var allServers = gameServerService.queryAllServersInNetwork();
        log.info("Found {} servers in network to register", allServers.size());

        for (GameServer gameServer : allServers) {
            try {
                this.registerServer(gameServer.serverId(), gameServer.hostAddress());
                serverCount++;

                if (gameServer.categoryType() == ServerCategoryType.LOBBY) {
                    lobbyCount++;
                    log.info("Registered LOBBY server: {} at {}:{}",
                      gameServer.serverId(),
                      gameServer.hostAddress().host(),
                      gameServer.hostAddress().port());
                }
            } catch (Exception e) {
                log.error("Failed to register server: {} at address: {}",
                  gameServer.serverId(),
                  gameServer.hostAddress(),
                  e);
            }
        }

        log.info("Successfully registered {} servers ({} lobbies) in the proxy platform", serverCount, lobbyCount);

        // Verificar se há lobbies disponíveis
        if (lobbyCount == 0) {
            log.warn("WARNING: No lobby servers were registered! Players will not be able to join.");
        }
    }

    @Override
    public void close() {
        log.info("Closing VelocityPlatform...");
    }

    @Override
    public void registerServer(@NotNull String serverId, @NotNull HostAddress address) {
        try {
            ServerInfo serverInfo = new ServerInfo(serverId, address.asInetAddress());
            proxyServer.registerServer(serverInfo);
            log.info("Registered server in Velocity: {} at address: {}:{}",
              serverId,
              address.host(),
              address.port());
        } catch (Exception e) {
            log.error("Failed to register server {} at address {}", serverId, address, e);
            throw e;
        }
    }

    @Override
    public void unregisterServer(@NotNull String serverId) {
        final var query = proxyServer.getServer(serverId);
        if (query.isEmpty()) {
            log.warn("Server with ID {} is not registered in the proxy, skipping unregistration.", serverId);
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

    /**
     * Obtém um servidor de lobby aleatório disponível
     *
     * @return GameServer de lobby aleatório ou null se nenhum estiver disponível
     */
    @Nullable
    GameServer getRandomLobby() {
        final List<GameServer> allLobbyServers = gameServerService.queryAllServersInNetwork()
          .stream()
          .filter(server -> server.categoryType() == ServerCategoryType.LOBBY)
          .toList();

        if (allLobbyServers.isEmpty()) {
            log.warn("No lobby servers available in GameServerService. Total servers: {}",
              gameServerService.queryAllServersInNetwork().size());
            return null;
        }

        GameServer selectedLobby = allLobbyServers.get(RANDOM.nextInt(allLobbyServers.size()));

        var velocityServer = proxyServer.getServer(selectedLobby.serverId());
        if (velocityServer.isEmpty()) {
            log.error("Selected lobby server {} is not registered in Velocity! Re-registering...",
              selectedLobby.serverId());

            // Tentar re-registrar o servidor
            try {
                registerServer(selectedLobby.serverId(), selectedLobby.hostAddress());
            } catch (Exception e) {
                log.error("Failed to re-register lobby server {}", selectedLobby.serverId(), e);
                return null;
            }
        }

        log.debug("Selected random lobby: {} from {} available lobbies",
          selectedLobby.serverId(),
          allLobbyServers.size());

        return selectedLobby;
    }
}
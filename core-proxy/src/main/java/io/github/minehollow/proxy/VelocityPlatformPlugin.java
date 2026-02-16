package io.github.minehollow.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.property.Property;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Plugin(
  id = "hollow-core-proxy",
  name = "hollow Core Proxy",
  version = "1.0.0", description =
  "Core plugin for Lugin Proxy"
)
@Slf4j
public class VelocityPlatformPlugin {
    private final ProxyServer proxyServer;
    private final VelocityPlatform velocityPlatform;

    // Flag para garantir que a inicialização foi concluída
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Inject
    public VelocityPlatformPlugin(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        velocityPlatform = new VelocityPlatform(proxyServer);
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        log.info("Initializing VelocityPlatform...");
        try {
            velocityPlatform.init();
            initialized.set(true);
            log.info("VelocityPlatform initialized successfully!");

            // Log de servidores disponíveis após inicialização
            logAvailableServers();
        } catch (Exception e) {
            log.error("Failed to initialize VelocityPlatform", e);
        }
    }

    @Subscribe
    public void onDefinitelyQuit(DisconnectEvent event) {
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        final var playerStateManager = PlayerNetworkStateManager.getInstance();
        final var playerState = playerStateManager.getPlayerState(event.getPlayer().getUniqueId());

        if (playerState != null) {
            playerStateManager.unregister(playerState);
            log.debug("Player state unregistered for: {}", event.getPlayer().getUsername());
        } else {
            log.warn("Player state for {} was null on disconnect.", event.getPlayer().getUsername());
        }
    }

    @Subscribe
    public void onPlayerGetKicked(KickedFromServerEvent event) {
        final var player = event.getPlayer();
        final var server = event.getServer();

        log.info("Player {} was kicked from server {}", player.getUsername(), server.getServerInfo().getName());

        GameServer byId = velocityPlatform.getGameServerService().getById(server.getServerInfo().getName());
        if (byId == null) {
            log.warn("Server {} not found in GameServerService", server.getServerInfo().getName());
            return;
        }

        if (byId.categoryType() == ServerCategoryType.LOBBY) {
            log.debug("Player was kicked from lobby, not redirecting");
            return;
        }

        final var lobbyServer = velocityPlatform.getRandomLobby();
        if (lobbyServer == null) {
            log.error("Player {} was kicked but no lobby server is available. Available servers: {}",
              player.getUsername(),
              velocityPlatform.getGameServerService().queryAllServersInNetwork().size());
            player.disconnect(Component.text("§cNenhum servidor de lobby disponível. Tente novamente mais tarde."));
            return;
        }

        final var query = proxyServer.getServer(lobbyServer.serverId());
        if (query.isEmpty()) {
            log.error("Player {} was kicked but the lobby server {} was not registered in Velocity. Registered servers: {}",
              player.getUsername(),
              lobbyServer.serverId(),
              proxyServer.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList());
            player.disconnect(Component.text("§cServidor de lobby não encontrado. Tente novamente mais tarde."));
            return;
        }

        log.info("Redirecting player {} to lobby server {}", player.getUsername(), lobbyServer.serverId());
        player.createConnectionRequest(query.get()).fireAndForget();
    }

    @Subscribe
    public void onProxyChooseServer(PlayerChooseInitialServerEvent event) {
        final Player player = event.getPlayer();

        // Verificar se a plataforma foi inicializada
        if (!initialized.get()) {
            log.error("Platform not initialized yet! Player {} cannot join.", player.getUsername());
            player.disconnect(Component.text("§cServidor ainda inicializando. Aguarde alguns segundos e tente novamente."));
            return;
        }

        final boolean searchForLobbyOnJoin = Property.getBoolean("SEARCH_FOR_LOBBY_ON_JOIN", true);
        if (!searchForLobbyOnJoin) {
            log.debug("SEARCH_FOR_LOBBY_ON_JOIN is disabled, skipping lobby selection for {}", player.getUsername());
            return;
        }

        log.info("Player {} is choosing initial server...", player.getUsername());

        final var lobbyServer = velocityPlatform.getRandomLobby();
        if (lobbyServer == null) {
            log.error("No lobby server available for player {}. Total servers in network: {}, Registered in Velocity: {}",
              player.getUsername(),
              velocityPlatform.getGameServerService().queryAllServersInNetwork().size(),
              proxyServer.getAllServers().size());

            // Log detalhado dos servidores
            logAvailableServers();

            player.disconnect(Component.text("§cNenhum servidor de lobby disponível no momento. Tente novamente mais tarde."));
            return;
        }

        log.info("Selected lobby server {} for player {}", lobbyServer.serverId(), player.getUsername());

        final var query = proxyServer.getServer(lobbyServer.serverId());
        if (query.isEmpty()) {
            log.error("Lobby server {} not registered in Velocity for player {}. Registered servers: {}",
              lobbyServer.serverId(),
              player.getUsername(),
              proxyServer.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList());

            player.disconnect(Component.text("§cServidor de lobby não encontrado. Tente novamente mais tarde."));
            return;
        }

        log.info("Successfully setting initial server to {} for player {}", lobbyServer.serverId(), player.getUsername());
        event.setInitialServer(query.get());
    }

    /**
     * Método auxiliar para logar informações sobre servidores disponíveis
     */
    private void logAvailableServers() {
        var allServers = velocityPlatform.getGameServerService().queryAllServersInNetwork();
        var lobbyServers = allServers.stream()
          .filter(s -> s.categoryType() == ServerCategoryType.LOBBY)
          .toList();

        log.info("=== Server Status ===");
        log.info("Total servers in GameServerService: {}", allServers.size());
        log.info("Lobby servers in GameServerService: {}", lobbyServers.size());
        log.info("Servers registered in Velocity: {}", proxyServer.getAllServers().size());

        log.info("Lobby servers details:");
        lobbyServers.forEach(server -> {
            boolean registeredInVelocity = proxyServer.getServer(server.serverId()).isPresent();
            log.info("  - {} ({}:{}) - Registered in Velocity: {}",
              server.serverId(),
              server.hostAddress().host(),
              server.hostAddress().port(),
              registeredInVelocity);
        });

        log.info("Velocity registered servers:");
        proxyServer.getAllServers().forEach(server ->
          log.info("  - {} ({}:{})",
            server.getServerInfo().getName(),
            server.getServerInfo().getAddress().getHostString(),
            server.getServerInfo().getAddress().getPort())
        );
        log.info("===================");
    }
}
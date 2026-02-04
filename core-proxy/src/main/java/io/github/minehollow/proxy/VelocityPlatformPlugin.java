package io.github.minehollow.proxy;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.property.Property;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;

@Plugin(
  id = "hollow-core-proxy",
  name = "hollow Core Proxy",
  version = "1.0.0", description =
  "Core plugin for Lugin Proxy"
)
@Slf4j
public class VelocityPlatformPlugin {
    private final ProxyServer proxyServer;

    private VelocityPlatform velocityPlatform;

    @Inject
    public VelocityPlatformPlugin(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        velocityPlatform = new VelocityPlatform(proxyServer);
        velocityPlatform.init();
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
    }

    @Subscribe
    public void onDefinitelyQuit(DisconnectEvent event) {

    }

//    @Subscribe(order = PostOrder.LATE)
//    public void onPermissionsSetup(PermissionsSetupEvent event, Continuation continuation) {
//        if (!(event.getSubject() instanceof Player p)) {
//            continuation.resume();
//            return;
//        }
//
//        PlayerAccountService service = this.velocityPlatform.getPlayerAccountService();
//        UUID playerId = p.getUniqueId();
//
//        String skin = null;
//        List<GameProfile.Property> properties = p.getGameProfile().getProperties();
//        for (var property : properties) {
//            if (property.getName().equals("textures")) {
//                skin = property.getValue();
//                break;
//            }
//        }
//
//        service.loadPlayerAccount(playerId, withDefaultAccount(createDefaultAccount(playerId, p.getUsername(), skin), true))
//            .thenAccept(account -> {
//                try {
//                    event.setProvider(new PlayerPermissionsProvider(p, account));
//                } finally {
//                    continuation.resume();
//                }
//            });
//    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        final var playerStateManager = PlayerNetworkStateManager.getInstance();
        final var playerState = playerStateManager.getPlayerState(event.getPlayer().getUniqueId());

        if (playerState != null) {
            playerStateManager.unregister(playerState);
        } else {
            log.warn("Player state for {} was null on disconnect.", event.getPlayer().getUsername());
        }
    }

    @Subscribe
    public void onPlayerGetKicked(KickedFromServerEvent event) {
        final var player = event.getPlayer();
        final var server = event.getServer();

        // TODO CHECKAR SE FOI PUNIDO!
        GameServer byId = velocityPlatform.getGameServerService().getById(server.getServerInfo().getName());
        if (byId == null || byId.categoryType() == ServerCategoryType.LOBBY) return;

        final var lobbyServer = velocityPlatform.getRandomLobby();
        if (lobbyServer == null) {
            log.debug("Player {} was kicked but no lobby server is available.", player.getUsername());
            return;
        }

        final var query = proxyServer.getServer(lobbyServer.serverId());
        if (query.isEmpty()) {
            log.debug("Player {} was kicked but the lobby server {} was not found.", player.getUsername(), lobbyServer.serverId());
            player.disconnect(Component.text("§cServidor de lobby não encontrado. Tente novamente mais tarde."));
            return;
        }

        player.createConnectionRequest(query.get()).fireAndForget();
    }

    @Subscribe
    public void onProxyChooseServer(PlayerChooseInitialServerEvent event) {
        final boolean searchForLobbyOnJoin = Property.getBoolean("SEARCH_FOR_LOBBY_ON_JOIN", false);
        if (!searchForLobbyOnJoin) {
            return;
        }

        final var player = event.getPlayer();
        final var lobbyServer = velocityPlatform.getRandomLobby();
        if (lobbyServer == null) {
            log.debug("Player {} attempted to join but no lobby server is available.", player.getUsername());
            player.disconnect(Component.text("§cNenhum servidor de lobby disponível no momento. Tente novamente mais tarde."));
            return;
        }

        final var query = proxyServer.getServer(lobbyServer.serverId());
        if (query.isEmpty()) {
            log.debug("Player {} attempted to join but the lobby server {} was not found.", player.getUsername(), lobbyServer.serverId());
            player.disconnect(Component.text("§cServidor de lobby não encontrado. Tente novamente mais tarde."));
            return;
        }

        event.setInitialServer(query.get());
    }

    public static final class PlayerPermissionsProvider implements PermissionProvider, PermissionFunction {
        private final Player player;
        private final PlayerAccount account;

        public PlayerPermissionsProvider(Player player, PlayerAccount account) {
            this.player = player;
            this.account = account;
        }

        @Override
        public PermissionFunction createFunction(PermissionSubject subject) {
            Preconditions.checkState(subject == this.player, "createFunction called with different argument");
            return this;
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            if (this.account == null || permission.equals("velocity.command.server")) {
                return Tristate.FALSE;
            }
            return this.account.hasPermission(permission) ? Tristate.TRUE : Tristate.UNDEFINED;
        }
    }
}

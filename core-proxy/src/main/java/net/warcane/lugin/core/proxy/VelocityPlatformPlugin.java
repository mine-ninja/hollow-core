package net.warcane.lugin.core.proxy;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

@Plugin(
  id = "lugin-core-proxy",
  name = "Lugin Core Proxy",
  version = "1.0.0", description =
  "Core plugin for Lugin Proxy"
)
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
    public void onProxyChooseServer(PlayerChooseInitialServerEvent event) {
        final var player = event.getPlayer();
        final var lobbyServer = velocityPlatform.getRandomLobby();
        if (lobbyServer == null) {
            player.disconnect(Component.text("§cNenhum servidor de lobby disponível no momento. Tente novamente mais tarde."));
            return;
        }

        final var query = proxyServer.getServer(lobbyServer.serverId());
        if (query.isEmpty()) {
            player.disconnect(Component.text("§cServidor de lobby não encontrado. Tente novamente mais tarde."));
            return;
        }

        event.setInitialServer(query.get());
    }
}

package io.github.minehollow.proxy.listener;

import io.github.minehollow.sdk.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import io.github.minehollow.proxy.VelocityPlatform;
import io.github.minehollow.sdk.server.GameServer;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.List;

@RequiredArgsConstructor
public class PlayerDirectPlayGameCategoryListener implements PacketListener<PlayerDirectPlayGameCategoryPacket> {
    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull PlayerDirectPlayGameCategoryPacket packet, @NotNull Headers headers) {
        final var categoryToSendPlayer = packet.categoryType();
        List<GameServer> servers = platform.getGameServerService().queryServersByCategoryType(categoryToSendPlayer)
          .stream()
          .filter(gameServer -> !gameServer.serverPlayers().isFull())
          .toList();

        if (servers.isEmpty()) {
            platform.sendMessageToPlayer(packet.playerId(), "§cNão há servidores disponíveis para o jogo selecionado. Aguarde ou tente novamente mais tarde.");
            return;
        }
        
        final var randomServer = servers.getFirst();
        platform.getProxyServer().getServer(randomServer.serverId()).ifPresent(serverToSend -> {
            platform.getProxyServer().getPlayer(packet.playerId()).ifPresent(player -> player.createConnectionRequest(serverToSend).fireAndForget());
        });
    }
}

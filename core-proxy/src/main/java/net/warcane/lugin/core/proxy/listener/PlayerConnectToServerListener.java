package net.warcane.lugin.core.proxy.listener;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectToServerPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.VelocityPlatform;
import net.warcane.lugin.core.server.GameServer;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PlayerConnectToServerListener implements PacketListener<PlayerConnectToServerPacket> {
    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull PlayerConnectToServerPacket packet, @NotNull Headers headers) {
        final var serverId = packet.serverId();
        GameServer server = platform.getGameServerService().getById(packet.serverId());

        if (server == null || server.serverPlayers().isFull()) {
            platform.sendMessageToPlayer(packet.playerId(), "§cO servidor não está disponível. Aguarde ou tente novamente mais tarde.");
            return;
        }

        platform.getProxyServer().getServer(serverId).ifPresent(serverToSend -> {
            platform.getProxyServer().getPlayer(packet.playerId()).ifPresent(player -> player.createConnectionRequest(serverToSend).fireAndForget());
        });
    }
}

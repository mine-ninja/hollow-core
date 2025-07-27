package net.warcane.lugin.core.proxy.listener;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.VelocityPlatform;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rok, Pedro Lucas nmm. Created on 27/07/2025
 * @project lugin-core
 */
@RequiredArgsConstructor
public class PlayerTeleportListener implements PacketListener<PlayerTeleportToLocationPacket> {

    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull PlayerTeleportToLocationPacket packet, @NotNull Headers headers) {
        final var serverId = packet.originLocation().targetServerId();
        platform.getProxyServer().getServer(serverId)
                .ifPresent(serverToSend -> {
                    platform.getProxyServer()
                            .getPlayer(packet.playerId())
                            .ifPresent(player -> player.createConnectionRequest(serverToSend).fireAndForget());
                });
    }
}

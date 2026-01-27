package io.github.minehollow.proxy.listener;

import lombok.RequiredArgsConstructor;
import io.github.minehollow.sdk.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import io.github.minehollow.sdk.player.teleport.PlayerJoinData;
import io.github.minehollow.sdk.player.teleport.PlayerJoinDataManager;
import io.github.minehollow.proxy.VelocityPlatform;
import org.jetbrains.annotations.NotNull;


@RequiredArgsConstructor
public class PlayerTeleportListener implements PacketListener<PlayerTeleportToLocationPacket> {

    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull PlayerTeleportToLocationPacket packet, @NotNull Headers headers) {
        final var playerId = packet.playerId();
        final var serverId = packet.targetLocation().targetServerId();
        final var targetLocation = packet.targetLocation();

        platform.getProxyServer().getServer(serverId)
                .ifPresent(serverToSend -> {
                    platform.getProxyServer()
                            .getPlayer(packet.playerId())
                            .ifPresent(player -> player.createConnectionRequest(serverToSend).fireAndForget());


                    PlayerJoinDataManager.getInstance().setJoinData(new PlayerJoinData(playerId, targetLocation));
                });
    }
}

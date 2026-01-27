package io.github.minehollow.proxy.listener;

import lombok.RequiredArgsConstructor;
import io.github.minehollow.sdk.network.packet.impl.server.ServerUnregisterPacket;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import io.github.minehollow.proxy.VelocityPlatform;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ServerUnregisterPacketListener implements PacketListener<ServerUnregisterPacket> {

    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull ServerUnregisterPacket packet, @NotNull Headers headers) {
        final var serverId = packet.serverId();
        platform.unregisterServer(serverId);
    }
}

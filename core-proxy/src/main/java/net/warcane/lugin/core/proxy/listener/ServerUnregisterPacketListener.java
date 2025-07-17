package net.warcane.lugin.core.proxy.listener;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.VelocityPlatform;
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

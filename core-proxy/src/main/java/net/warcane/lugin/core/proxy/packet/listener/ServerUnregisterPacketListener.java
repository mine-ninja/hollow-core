package net.warcane.lugin.core.proxy.packet.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.BungeecordPlatform;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class ServerUnregisterPacketListener implements PacketListener<ServerUnregisterPacket> {

    private final BungeecordPlatform platform;

    @Override
    public void onReceivePacket(@NotNull ServerUnregisterPacket packet, @NotNull Headers headers) {
        final var serverId = packet.serverId();
        final var serverAddress = headers.serverOriginAddress();

        platform.unregisterServer(serverId);
        log.info("Unregistered server {} at address {}", serverId, serverAddress);
    }
}

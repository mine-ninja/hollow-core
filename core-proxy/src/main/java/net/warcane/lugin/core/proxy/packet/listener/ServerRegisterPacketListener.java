package net.warcane.lugin.core.proxy.packet.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.BungeecordPlatform;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class ServerRegisterPacketListener implements PacketListener<ServerRegisterPacket> {

    private final BungeecordPlatform platform;

    @Override
    public void onReceivePacket(@NotNull ServerRegisterPacket packet, @NotNull Headers headers) {
        final var serverId = packet.serverId();
        final var address = packet.hostAddress();

        platform.registerServer(serverId, address);
        log.info("Server registered: {} at {}", serverId, address);
    }
}

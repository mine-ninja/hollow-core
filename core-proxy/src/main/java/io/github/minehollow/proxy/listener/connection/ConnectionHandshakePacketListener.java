package io.github.minehollow.proxy.listener.connection;

import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.github.minehollow.sdk.connection.ConnectionStatus;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ConnectionHandshakePacketListener implements PacketListener<ConnectionHandshakePacket> {

    private final ProxyServer proxyServer;

    @Override
    public void onReceivePacket(@NotNull ConnectionHandshakePacket packet, @NotNull Headers headers) {
        if (packet.getStatus() == ConnectionStatus.PENDING) return;

        var optPlayer = proxyServer.getPlayer(packet.getUserId());
        if (optPlayer.isEmpty()) return;

        var player = optPlayer.get();
        var serverId = packet.getTargetServerId();
        if (serverId == null || serverId.isEmpty()) {
            player.sendMessage(Component.text("§cO servidor destino não foi encontrado ou está indisponível!"));
            return;
        }

        var server = proxyServer.getServer(serverId);
        if (server.isEmpty() || packet.getStatus() == ConnectionStatus.DENIED) {
            player.sendMessage(Component.text(packet.getFallbackMessage()).color(NamedTextColor.RED));
        } else {
            player.createConnectionRequest(server.get()).fireAndForget();
        }
    }
}

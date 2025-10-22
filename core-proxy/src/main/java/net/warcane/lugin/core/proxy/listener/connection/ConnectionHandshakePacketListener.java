package net.warcane.lugin.core.proxy.listener.connection;

import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.warcane.lugin.core.connection.ConnectionStatus;
import net.warcane.lugin.core.network.packet.impl.connection.ConnectionHandshakePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ConnectionHandshakePacketListener implements PacketListener<ConnectionHandshakePacket> {

    private final ProxyServer proxyServer;

    @Override
    public void onReceivePacket(@NotNull ConnectionHandshakePacket packet, @NotNull Headers headers) {
        if (packet.status() == ConnectionStatus.PENDING) return;

        var optPlayer = proxyServer.getPlayer(packet.userId());
        if (optPlayer.isEmpty()) return;

        var player = optPlayer.get();
        var serverId = packet.targetServerId();
        if (serverId == null || serverId.isEmpty()) {
            player.sendMessage(Component.text("§cO servidor destino não foi encontrado ou está indisponível!"));
            return;
        }

        var server = proxyServer.getServer(serverId);
        if (server.isEmpty() || packet.status() == ConnectionStatus.DENIED) {
            player.sendMessage(Component.text(packet.fallbackMessage()).color(NamedTextColor.RED));
        } else {
            player.createConnectionRequest(server.get()).fireAndForget();
        }

    }
}

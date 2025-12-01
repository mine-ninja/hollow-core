package net.warcane.lugin.core.proxy.listener;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectToSubCategoryPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.proxy.VelocityPlatform;
import net.warcane.lugin.core.server.GameServer;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PlayerConnectToSubCategoryListener implements PacketListener<PlayerConnectToSubCategoryPacket> {
    private final VelocityPlatform platform;
    
    @Override
    public void onReceivePacket(@NotNull PlayerConnectToSubCategoryPacket packet, @NotNull Headers headers) {
        final var categoryToSendPlayer = packet.subCategoryType();
        List<GameServer> servers = platform.getGameServerService().queryAllServersInNetwork().stream()
            .filter(gameServer -> gameServer.subCategory() == categoryToSendPlayer)
            .filter(gameServer -> !gameServer.serverPlayers().isFull())
            .sorted(Comparator.comparingInt(value -> value.serverPlayers().online()))
            .toList();
        
        if (servers.isEmpty()) {
            platform.sendMessageToPlayer(packet.playerId(), "§cNão há servidores disponíveis. Aguarde ou tente novamente mais tarde.");
            return;
        }
        
        Optional<RegisteredServer> server = platform.getProxyServer().getServer(servers.getFirst().serverId());
        server.ifPresent(s -> platform.getProxyServer().getPlayer(packet.playerId()).ifPresent(player -> player.createConnectionRequest(s).fireAndForget()));
    }
}

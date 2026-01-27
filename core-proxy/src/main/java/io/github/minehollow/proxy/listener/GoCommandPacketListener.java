package io.github.minehollow.proxy.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.staff.GoCachePacket;
import io.github.minehollow.sdk.network.packet.impl.staff.GoCommandPacket;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import io.github.minehollow.proxy.VelocityPlatform;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Meiallu. Created on 19/08/2025
 * @project lugin-core
 */
@RequiredArgsConstructor
public class GoCommandPacketListener implements PacketListener<GoCommandPacket> {

    private static final String COMMAND_SUCCESS = "§aEnviando você à %s...";
    private static final String PLAYER_NOT_FOUND = "§cO jogador inserido não foi encontrado.";
    private static final String SERVER_NOT_FOUND = "§cO servidor do jogador inserido não foi encontrado.";

    private final VelocityPlatform platform;

    @Override
    public void onReceivePacket(@NotNull GoCommandPacket packet, @NotNull Headers headers) {
        platform.getProxyServer().getPlayer(packet.uniqueId()).ifPresent(player -> {
            Optional<Player> targetOptional = platform.getProxyServer().getPlayer(packet.targetName());

            if (targetOptional.isPresent()) {
                Player target = targetOptional.get();
                Optional<ServerConnection> serverConnectionOptional = target.getCurrentServer();

                if (serverConnectionOptional.isPresent()) {
                    ServerConnection serverConnection = serverConnectionOptional.get();

                    player.sendMessage(Component.text(COMMAND_SUCCESS.formatted(target.getUsername())));

                    GoCachePacket goCachePacket = new GoCachePacket(packet.uniqueId(), target.getUniqueId());
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.GO, goCachePacket);

                    player.createConnectionRequest(serverConnection.getServer()).fireAndForget();
                } else {
                    player.sendMessage(Component.text(SERVER_NOT_FOUND));
                }
            } else {
                player.sendMessage(Component.text(PLAYER_NOT_FOUND));
            }
        });
    }
}

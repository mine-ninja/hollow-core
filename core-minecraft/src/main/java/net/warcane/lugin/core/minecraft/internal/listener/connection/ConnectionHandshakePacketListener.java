package net.warcane.lugin.core.minecraft.internal.listener.connection;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.connection.ConnectionStatus;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.connection.ConnectionRequestEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.network.packet.impl.connection.ConnectionHandshakePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@RequiredArgsConstructor
public class ConnectionHandshakePacketListener implements PacketListener<ConnectionHandshakePacket> {

    private final BukkitPlatform platform;

    @Override
    public void onReceivePacket(@NotNull ConnectionHandshakePacket packet, @NotNull Headers headers) {
        if (packet.getStatus() == ConnectionStatus.DENIED) return;
        if (packet.getStatus() == ConnectionStatus.PENDING) {
            if (packet.getTargetServerId() != null && !Objects.equals(packet.getTargetServerId(), platform.getGameServer().serverId())) {
                return;
            }

            if (packet.getTargetId() != null) {
                var targetPlayer = Bukkit.getPlayer(packet.getTargetId());
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    return;
                }
            }

            if (packet.getTargetName() != null) {
                var targetPlayer = Bukkit.getPlayer(packet.getTargetName());
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    return;
                } else {
                    packet.setTargetId(targetPlayer.getUniqueId());
                    packet.setTargetServerId(platform.getGameServer().serverId());
                }
            }

            var event = new ConnectionRequestEvent(packet.getUserId(), ConnectionRequestEvent.Side.CURRENT, packet);
            Bukkit.getPluginManager().callEvent(event);

            Tasks.runSync(
                () -> {
                    if (event.isCancelled()) {
                        packet.setFallbackMessage(event.getFallbackMessage());
                        packet.setStatus(ConnectionStatus.DENIED);
                    } else {
                        packet.setStatus(ConnectionStatus.ALLOWED);
                        platform.getTeleportManager().addPendingRequest(packet.getUserId(), packet);
                    }

                    platform.getTeleportManager().sendConnectionRequest(packet);
                });
        }
    }
}

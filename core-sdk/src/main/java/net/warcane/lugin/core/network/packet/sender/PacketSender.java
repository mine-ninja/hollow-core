package net.warcane.lugin.core.network.packet.sender;

import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.network.packet.data.SerializedPacketData;
import net.warcane.lugin.core.network.packet.listener.PacketListener.Headers;
import net.warcane.lugin.core.util.JsonUtil;
import net.warcane.lugin.core.util.address.HostAddress;
import net.warcane.lugin.core.util.compress.ZstdUtil;
import org.jetbrains.annotations.NotNull;

public final class PacketSender {
    final Platform platform;
    final HostAddress hostAddress;
    final RedisConnector connector;

    public PacketSender(Platform platform, HostAddress hostAddress, RedisConnector connector) {
        this.platform = platform;
        this.hostAddress = hostAddress;
        this.connector = connector;
    }

    public void sendPacket(@NotNull NetworkChannel channel, @NotNull NetworkPacket packet) {
        final var serializedPacketData = serializePacket(channel, packet);
        final byte[] serializedData = JsonUtil.toJsonBytes(serializedPacketData);

        connector.useJedis(jedis -> jedis.publish(channel.name().getBytes(), serializedData));
    }

    private SerializedPacketData serializePacket(@NotNull NetworkChannel channel, @NotNull NetworkPacket packet) {
        final int packetTypeId = NetworkPacket.idOf(packet.getClass());

        final byte[] rawPacketData = JsonUtil.toJsonBytes(packet);
        final int originalPacketLength = rawPacketData.length;

        final var headers = new Headers(platform.getId(), hostAddress, channel);
        final var serializedHeaders = JsonUtil.toJsonBytes(headers);

        final byte[] compressedPacketData = ZstdUtil.compress(rawPacketData);

        return new SerializedPacketData(packetTypeId, originalPacketLength, serializedHeaders,
          compressedPacketData);
    }
}

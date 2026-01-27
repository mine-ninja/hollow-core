package io.github.minehollow.sdk.network.packet.sender;

import io.github.minehollow.sdk.Platform;
import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import io.github.minehollow.sdk.network.packet.data.SerializedPacketData;
import io.github.minehollow.sdk.network.packet.listener.PacketListener.Headers;
import io.github.minehollow.sdk.util.JsonUtil;
import io.github.minehollow.sdk.util.address.HostAddress;
import io.github.minehollow.sdk.util.compress.ZstdUtil;
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

package io.github.minehollow.sdk.network;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import io.github.minehollow.sdk.network.packet.data.SerializedPacketData;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import io.github.minehollow.sdk.network.packet.listener.PacketListenerMap;
import io.github.minehollow.sdk.util.JsonUtil;
import io.github.minehollow.sdk.util.compress.ZstdUtil;
import redis.clients.jedis.BinaryJedisPubSub;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
@RequiredArgsConstructor
final class InternalPubSub extends BinaryJedisPubSub {


    private final ExecutorService executorService;
    private final RedisConnector connector;
    private final PacketListenerMap packetListenerMap;
    private final List<NetworkChannel> channels;

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        executorService.execute(() -> {
            if (!containsChannel(channel)) return;

            try {
                final var packetData = JsonUtil.fromJson(message, SerializedPacketData.class);
                final int packetTypeId = packetData.packetTypeId();

                List<PacketListener> listenersForPacket = packetListenerMap.getListenersForPacket(packetTypeId);
                if (listenersForPacket == null || listenersForPacket.isEmpty()) return;

                final var packetClass = NetworkPacket.classOf(packetTypeId);
                if (packetClass == null) {
                    throw new IllegalArgumentException("No packet class found for ID: " + packetTypeId);
                }

                final var headers = JsonUtil.fromJson(packetData.headers(), PacketListener.Headers.class);
                if (!channels.contains(headers.channel())) {
                    log.warn("Received packet on channel {} that is not registered in the client: {}", new String(channel), headers.channel().name());
                    return;
                }

                final int originalPacketLength = packetData.originalPacketLength();
                final byte[] compressedPacketData = packetData.compressedPacketData();
                final byte[] decompressedPacketData = ZstdUtil.decompress(compressedPacketData, originalPacketLength);
                final var packet = JsonUtil.fromJson(decompressedPacketData, packetClass);

                for (PacketListener packetListener : listenersForPacket) {
                    packetListener.onReceivePacket(packet, headers);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to process message on channel: " + new String(channel), e);
            }
        });
    }

    private boolean containsChannel(byte[] channel) {
        for (NetworkChannel networkChannel : channels) {
            final var chNameAsBytes = networkChannel.name().getBytes();
            if (Arrays.equals(chNameAsBytes, channel)) {
                return true;
            }
        }
        return false;
    }
}

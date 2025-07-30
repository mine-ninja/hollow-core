package net.warcane.lugin.core.network;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.network.packet.listener.PacketListenerMap;
import net.warcane.lugin.core.network.packet.sender.PacketSender;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class NetworkClient {

    protected final Platform platform;
    protected final HostAddress hostAddres;
    protected final ExecutorService executorService;
    protected final RedisConnector redisConnector;

    protected final PacketSender packetSender;

    private final PacketListenerMap packetListenerMap = new PacketListenerMap();

    private Thread subscriptionThread;

    public NetworkClient(@NotNull Platform platform, @NotNull HostAddress address, @NotNull ExecutorService executorService) {
        this.platform = platform;
        this.hostAddres = address;
        this.executorService = executorService;
        this.redisConnector = RedisConnector.getInstance();
        this.packetSender = new PacketSender(platform, address, redisConnector);
    }


    public void subscribeToChannels(NetworkChannel... channels) {
        if (subscriptionThread != null) {
            throw new IllegalStateException("NetworkClient is already initialized.");
        }

        final byte[][] networkChannelNames = Arrays.stream(channels)
          .map(NetworkChannel::name)
          .map(String::getBytes)
          .toArray(byte[][]::new);

        this.subscriptionThread = new Thread(() -> redisConnector.useJedis(jedis -> {
            final var pubSub = new InternalPubSub(executorService, redisConnector, packetListenerMap, List.of(channels));
            jedis.subscribe(pubSub, networkChannelNames);
        }), "NetworkClient-SubscriptionThread");

        this.subscriptionThread.setDaemon(true);
        this.subscriptionThread.start();
    }

    public void sendNetworkPacket(@NotNull NetworkChannel channel, @NotNull NetworkPacket packet) {
        log.info("Sending packet {}", NetworkPacket.idOf(packet.getClass()));
        executorService.execute(() -> packetSender.sendPacket(channel, packet));
    }

    public <T extends NetworkPacket> void registerPacketListener(Class<T> packetClazz, PacketListener<T> listener) {
        packetListenerMap.registerListener(packetClazz, listener);
    }
}

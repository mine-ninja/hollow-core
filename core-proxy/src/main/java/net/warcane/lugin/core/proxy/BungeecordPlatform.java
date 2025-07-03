package net.warcane.lugin.core.proxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.ProxyPlatform;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.proxy.packet.listener.ServerRegisterPacketListener;
import net.warcane.lugin.core.proxy.packet.listener.ServerUnregisterPacketListener;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

public class BungeecordPlatform extends AbstractPlatform implements ProxyPlatform {

    private static ListenerInfo primaryListener;

    private final Plugin plugin;
    private final NetworkClient networkClient;

    public BungeecordPlatform(@NotNull Plugin plugin) {
        super(RedisConnector.fromInternalProperties());

        this.plugin = plugin;
        this.networkClient = new NetworkClient(this, localAddress(), executorService);
    }

    @Override
    public void init(NetworkChannel... channels) {
        networkClient.subscribeToChannels(channels);
        networkClient.registerPacketListener(ServerRegisterPacket.class, new ServerRegisterPacketListener(this));
        networkClient.registerPacketListener(ServerUnregisterPacket.class, new ServerUnregisterPacketListener(this));
    }

    @Override
    public void close() {

    }

    @Override
    public void registerServer(@NotNull String serverId, @NotNull HostAddress address) {
        final var serverInfo = plugin.getProxy()
          .constructServerInfo(serverId, address.asInetAddress(), "", false);

        plugin.getProxy().getServers().put(serverId, serverInfo);
    }

    @Override
    public void unregisterServer(@NotNull String serverId) {
        plugin.getProxy().getServers().remove(serverId);
    }


    @Override
    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public static ListenerInfo getPrimaryListener() {
        if (primaryListener == null) {
            primaryListener = ProxyServer.getInstance()
              .getConfig()
              .getListeners()
              .iterator()
              .next();
        }
        return primaryListener;
    }

    private static HostAddress localAddress() {
        return HostAddress.fromInetSocketAddress((InetSocketAddress) getPrimaryListener().getSocketAddress());
    }
}

package net.warcane.lugin.core.minecraft;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.MinecraftServerPlatform;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.listener.InternalPlayerListener;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.player.PlayerCount;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static net.warcane.lugin.core.minecraft.task.Tasks.runAsyncLater;

@Slf4j
public class BukkitPlatform extends AbstractPlatform implements MinecraftServerPlatform {

    /**
     * Verifica se a plataforma Bukkit está inicializada.
     *
     * @return true se a plataforma estiver inicializada, false caso contrário.
     */
    public static boolean isInitialized() {
        return Bukkit.getServicesManager().isProvidedFor(Platform.class);
    }

    /**
     * Obtém a instância da plataforma Bukkit.
     *
     * @return a instância de BukkitPlatform.
     * @throws IllegalStateException se a plataforma não estiver inicializada ou não estiver disponível.
     */
    @NotNull
    public static BukkitPlatform getInstance() {
        final var platform = Bukkit.getServicesManager().load(Platform.class);
        if (platform instanceof BukkitPlatform bukkitPlatform) {
            return bukkitPlatform;
        }
        throw new IllegalStateException("BukkitPlatform is not initialized or not available.");
    }


    private final Plugin plugin;
    private final HostAddress hostAddress;
    private final ServerCategoryType serverCategoryType;

    private final NetworkClient networkClient;

    private boolean online;

    public BukkitPlatform(Plugin plugin, @NotNull ServerCategoryType serverCategoryType) {
        super(RedisConnector.fromInternalProperties());

        this.plugin = plugin;
        this.hostAddress = HostAddress.localAddress((short) Bukkit.getPort());
        this.serverCategoryType = serverCategoryType;
        this.networkClient = new NetworkClient(this, hostAddress, executorService);

        Bukkit.getServicesManager().register(Platform.class, this, plugin, org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public void init(@NotNull NetworkChannel... channels) {
        log.info("Initializing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());

        networkClient.subscribeToChannels(channels);
        log.info("Bukkit Platform initialized with channels: {}", Arrays.toString(channels));

        final var serverRegisterPacket = new ServerRegisterPacket(this.getId(), serverCategoryType, hostAddress);
        runAsyncLater(() -> networkClient.sendPacket(NetworkChannel.SERVER_STATUS, serverRegisterPacket), 20);

        Bukkit.getPluginManager().registerEvents(new InternalPlayerListener(this), plugin);

        this.online = true;
        gameServerService.update(this.getGameServer().withOnlineStatus(true));
    }

    @Override
    public void close() {
        log.info("Closing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());
        networkClient.sendPacket(NetworkChannel.SERVER_STATUS, new ServerUnregisterPacket(this.getId()));
    }

    @Override
    public @NotNull NetworkClient getNetworkClient() {
        return networkClient;
    }

    @Override
    public @NotNull ServerCategoryType getServerCategoryType() {
        return serverCategoryType;
    }

    @Override
    @Contract(pure = true)
    public @NotNull PlayerCount getPlayerCount() {
        return new PlayerCount(Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers());
    }

    @Override
    public @NotNull HostAddress getServerHostAddress() {
        return hostAddress;
    }

    @Override
    public double[] getTps() {
        return Bukkit.getServer().spigot().getTPS();
    }

    public GameServer getGameServer() {
        return new GameServer(this.getId(), this.getServerCategoryType(), this.getServerHostAddress(),
          this.getPlayerCount(), this.online);
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }
}

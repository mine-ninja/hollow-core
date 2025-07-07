package net.warcane.lugin.core.minecraft;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.MinecraftServerPlatform;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.minecraft.internal.command.InternalCommandManager;
import net.warcane.lugin.core.minecraft.internal.listener.InternalPacketListeners;
import net.warcane.lugin.core.minecraft.internal.listener.InternalPlayerListener;
import net.warcane.lugin.core.minecraft.permission.NmsPermissionInjector;
import net.warcane.lugin.core.minecraft.permission.PermissionInjector;
import net.warcane.lugin.core.minecraft.util.team.NametagAPI;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.ServerPlayerCount;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

import static net.warcane.lugin.core.minecraft.task.Tasks.runAsyncLater;

@Slf4j
public class BukkitPlatform extends AbstractPlatform implements MinecraftServerPlatform {

    /**
     * Cria uma instância da plataforma Bukkit (Se não existir) e a registra no Bukkit ServicesManager.
     *
     * @param plugin       o plugin Bukkit associado a esta plataforma.
     * @return a instância de BukkitPlatform.
     */
    public static BukkitPlatform provide(@NotNull Plugin plugin) {
        final var rawType = Property.getOrThrow("SERVER_TYPE");
        final var categoryType = ServerCategoryType.fromName(rawType);

        if (isInitialized()) {
            final var currentInstance = getInstance();
            if (currentInstance.getServerCategoryType() != categoryType) {
                log.warn("Attempted to provide a BukkitPlatform with a different category type. Current: {}, Provided: {}",
                  currentInstance.getServerCategoryType(), categoryType);
            }
            return currentInstance;
        }
        return new BukkitPlatform(plugin, categoryType);
    }

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
    private final ServerCategoryType serverCategoryType;
    private final InternalCommandManager internalCommandManager;
    private final PermissionInjector permissionInjector;

    private boolean online;

    private BukkitPlatform(Plugin plugin, @NotNull ServerCategoryType serverCategoryType) {
        super(HostAddress.localAddress((short) Bukkit.getPort()));

        this.plugin = plugin;
        this.serverCategoryType = serverCategoryType;
        this.internalCommandManager = new InternalCommandManager(this);
        this.permissionInjector = new NmsPermissionInjector(this);

        this.loadGroupPermissions();

        NametagAPI.registerApi(plugin);
        Bukkit.getServicesManager().register(Platform.class, this, plugin, ServicePriority.Normal);
    }

    @Override
    public void init(@NotNull NetworkChannel... channels) {
        internalCommandManager.registerInternalCommands();

        log.info("Initializing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());

        networkClient.subscribeToChannels(channels);
        log.info("Bukkit Platform initialized with channels: {}", Arrays.toString(channels));

        final var serverRegisterPacket = new ServerRegisterPacket(this.getId(), serverCategoryType, hostAddress);
        runAsyncLater(() -> networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, serverRegisterPacket), 20);

        Bukkit.getPluginManager().registerEvents(new InternalPlayerListener(this), plugin);

        final var internalPackets = new InternalPacketListeners(this);
        internalPackets.setup();

        this.online = true;
        gameServerService.update(this.getGameServer().withOnlineStatus(true));

        log.info("Bukkit Platform is now online with ID: {}", this.getId());
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateServerInfo, 20, 20 * 10);
    }

    @Override
    public void close() {
        log.info("Closing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());
        networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, new ServerUnregisterPacket(this.getId()));
    }

    @Override
    public @NotNull ServerCategoryType getServerCategoryType() {
        return serverCategoryType;
    }

    @Override
    @Contract(pure = true)
    public @NotNull ServerPlayerCount getPlayerCount() {
        return new ServerPlayerCount(Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers());
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
        return new GameServer(this.getId(), this.getServerCategoryType(), this.getServerHostAddress(), this.getPlayerCount(), this.online);
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    public PermissionInjector getPermissionInjector() {
        return permissionInjector;
    }

    public void updateServerInfo() {
        if (!online) return;

        gameServerService.update(this.getGameServer());
    }

    public void tryConnectPlayerToServerCategory(@NotNull UUID player, @NotNull ServerCategoryType categoryType) {
        final var packet = new PlayerDirectPlayGameCategoryPacket(player, categoryType);
        networkClient.sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);
    }
}

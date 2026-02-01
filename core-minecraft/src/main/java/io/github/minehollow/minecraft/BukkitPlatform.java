package io.github.minehollow.minecraft;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.minehollow.minecraft.centralcart.CentralCart;
import io.github.minehollow.minecraft.currency.CurrencyManager;
import io.github.minehollow.minecraft.discord.DiscordService;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.gamerule.GameRuleManager;
import io.github.minehollow.minecraft.gamerule.listener.WorldLoadListener;
import io.github.minehollow.minecraft.internal.command.InternalCommandManager;
import io.github.minehollow.minecraft.internal.listener.*;
import io.github.minehollow.minecraft.internal.listener.connection.ConnectionHandshakePacketListener;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.nametag.NameTagHandler;
import io.github.minehollow.minecraft.nametag.resolver.LegacyNameTagResolver;
import io.github.minehollow.minecraft.nametag.resolver.ModernNameTagResolver;
import io.github.minehollow.minecraft.nametag.resolver.NameTagResolver;
import io.github.minehollow.minecraft.permission.PermissionInjector;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.teleport.TeleportManager;
import io.github.minehollow.minecraft.teleport.TeleportTrafficListener;
import io.github.minehollow.minecraft.util.message.AdventureFormatters;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.vanish.VanishManager;
import io.github.minehollow.sdk.AbstractPlatform;
import io.github.minehollow.sdk.MinecraftServerPlatform;
import io.github.minehollow.sdk.Platform;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.connection.ConnectionHandshakePacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToSubCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendSoundToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerRegisterPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerUnregisterPacket;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.player.subscription.SubscriptionCategoryType;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.ServerPlayers;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.server.type.ServerSubCategoryType;
import io.github.minehollow.sdk.util.address.HostAddress;
import io.github.minehollow.sdk.util.property.Property;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.event.EventBus;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class BukkitPlatform extends AbstractPlatform implements MinecraftServerPlatform {

    private static final ScheduledExecutorService SINGLE_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(
      runnable -> new Thread(runnable, "BukkitPlatform-SingleExecutor")
    );

    /**
     * Cria uma instância da plataforma Bukkit (Se não existir) throwable a registra no Bukkit ServicesManager.
     *
     * @param plugin o plugin Bukkit associado a esta plataforma.
     * @return a instância de BukkitPlatform.
     */
    public static BukkitPlatform provide(@NotNull JavaPlugin plugin) {
        final var rawType = Property.get("SERVER_TYPE", "UNKNOWN");
        final var categoryType = ServerCategoryType.fromName(rawType);
        if (!isInitialized()) {
            return new BukkitPlatform(plugin, categoryType);
        }

        final var currentInstance = getInstance();
        if (currentInstance.getServerCategoryType() != categoryType) {
            log.warn("Attempted to provide a BukkitPlatform with a different category type. Current: {}, Provided: {}",
              currentInstance.getServerCategoryType(), categoryType);
        }
        return currentInstance;
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
    private final ServerSubCategoryType serverSubCategoryType;
    private final InternalCommandManager internalCommandManager;
    private final PermissionInjector permissionInjector;
    private final CurrencyManager currencyManager;
    private final VanishManager vanishManager;
    private final SimpleMenuManager menuManager;
    private final GameRuleManager gameRuleManager;
    private final TeleportManager teleportManager;

    @Getter
    private final NameTagHandler nameTagHandler;

    @Getter
    private NameTagResolver nameTagResolver;
    @Getter
    private CentralCart centralCart;

    @Getter
    private final DiscordService discordService;

    private boolean online;

    private BukkitPlatform(JavaPlugin plugin, @NotNull ServerCategoryType serverCategoryType) {
        super(HostAddress.localAddress((short) Bukkit.getPort()));

        this.plugin = plugin;
        this.serverCategoryType = serverCategoryType;
        this.serverSubCategoryType = Property.getEnum("SERVER_SUB_CATEGORY", ServerSubCategoryType.class, ServerSubCategoryType.NONE);
        this.internalCommandManager = new InternalCommandManager(this);
        this.permissionInjector = PermissionInjector.fromCurrentPlatform(this);
        this.currencyManager = new CurrencyManager(this);
        this.vanishManager = new VanishManager(this);
        this.menuManager = new SimpleMenuManager(this);

        this.gameRuleManager = new GameRuleManager(this);

        this.teleportManager = new TeleportManager(this);

        this.centralCart = new CentralCart();
        this.centralCart.initSocket();

        final var usesModernTags = Property.getBoolean("USE_MODERN_TAGS", false);
        if (usesModernTags) {
            this.nameTagResolver = new ModernNameTagResolver(this);
        } else {
            this.nameTagResolver = new LegacyNameTagResolver(this);
        }

        this.discordService = new DiscordService(this);
        this.nameTagHandler = new NameTagHandler(this);

        this.loadGroupPermissions();
        Bukkit.getServicesManager().register(Platform.class, this, plugin, ServicePriority.Normal);
    }

    @Override
    public void init(@NotNull NetworkChannel... channels) {
        SINGLE_EXECUTOR_SERVICE.scheduleAtFixedRate(
          AsyncServerTickEvent::call,
          0, 50, TimeUnit.MILLISECONDS
        );

        menuManager.initialize();

        internalCommandManager.registerInternalCommands();

        gameRuleManager.initialize();
        Bukkit.getPluginManager().registerEvents(new WorldLoadListener(this), plugin);

        log.info("Initializing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());

        networkClient.subscribeToChannels(channels);
        log.info("Bukkit Platform initialized with channels: {}", Arrays.toString(channels));

        final var serverRegisterPacket = new ServerRegisterPacket(this.getId(), serverCategoryType, hostAddress);
        networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, serverRegisterPacket);

        Bukkit.getPluginManager().registerEvents(new InternalPlayerListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerGroupUpdatingListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new StaffTrackingListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerPermissionUpdatingListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new TeleportTrafficListener(this), plugin);

        ChatInput.init(plugin);

        AdventureFormatters.init();

        final var internalPackets = new InternalPacketListeners(this);
        internalPackets.setup();

        networkClient.registerPacketListener(ConnectionHandshakePacket.class, new ConnectionHandshakePacketListener(this));

        this.online = true;

        gameServerService.update(this.getGameServer().withOnlineStatus(true));
        log.info("Bukkit Platform is now online with ID: {}, Category: {} and SubCategory: {}", this.getId(), this.getServerCategoryType(), this.getServerSubCategoryType());
        Tasks.runAsyncRepeating(this::updateServerInfo, 20, 20 * 10);
        Bukkit.getConsoleSender().sendMessage("§aCarregando nomes de jogadores para o redis (para acesso rápido)");


        Tasks.runAsyncLater(() -> {
            EventBus eventBus = TabAPI.getInstance().getEventBus();
            if (eventBus == null) {
                return;
            }

            eventBus.register(
              TabLoadEvent.class,
              event -> Tasks.runAsyncLater(nameTagHandler::updateAll, 5)
            );
        }, 20);
    }

    @Override
    public void close() {
        log.info("Closing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());
        networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, new ServerUnregisterPacket(this.getId()));

        PlayerNetworkStateManager stateManager = PlayerNetworkStateManager.getInstance();
        stateManager.getOnlinePlayersInServer(this.getId()).forEach(stateManager::unregister);

        if (this.centralCart != null) {
            this.centralCart.disableSocket();
        }
    }


    @Override
    public @NotNull ServerCategoryType getServerCategoryType() {
        return serverCategoryType;
    }

    @Override
    public @NotNull ServerSubCategoryType getServerSubCategoryType() {
        return this.serverSubCategoryType;
    }

    @Override
    @Contract(pure = true)
    public @NotNull ServerPlayers getPlayerCount() {
        return new ServerPlayers(
          Bukkit.getOnlinePlayers().size(),
          Bukkit.getMaxPlayers()
        );
    }

    @Override
    public @NotNull HostAddress getServerHostAddress() {
        return hostAddress;
    }

    @Override
    public double[] getTps() {
        try {
            return Bukkit.getServer().getTPS();
        } catch (Exception e) {
            return new double[]{20, 20, 20}; // fallback sem errors.
        }
    }

    public GameServer getGameServer() {
        return new GameServer(this.getId(), this.getServerCategoryType(), this.getServerSubCategoryType(), this.getServerHostAddress(), this.getPlayerCount(), this.online);
    }

    public boolean isGroupAllowedToJoin(@NotNull PlayerGroup playerGroup) {
        final var allowedGroups = Property.get("ALLOWED_GROUPS"); // "ADMIN,MOD,TRIAL,DEFAULT"
        if (allowedGroups == null) return true;

        return allowedGroups.contains(playerGroup.name());
    }

    public String getDisallowJoinMessage() {
        return Property.get("DISALLOW_JOIN_MESSAGE", "§cVocê não tem permissão para entrar neste servidor.");
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

    /**
     * Tenta conectar um jogador a uma subcategoria de servidor específica.
     * O jogador vai ser conectado ao servidor com menos jogadores online dentro da subcategoria.
     */
    public void tryConnectPlayerToSubCategory(@NotNull UUID player, @NotNull ServerSubCategoryType subCategoryType) {
        final var packet = new PlayerConnectToSubCategoryPacket(player, subCategoryType);
        networkClient.sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);
    }

    public void tryConnectPlayerToServer(@NotNull UUID player, @NotNull String serverId) {
        final var packet = new PlayerConnectToServerPacket(player, serverId);
        networkClient.sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);
    }

    public void tryToPlaySoundToPlayer(@NotNull UUID playerId, @NotNull String soundKey, float volume, float pitch) {
        final var sendSoundPacket = new SendSoundToPlayerPacket(playerId, soundKey, volume, pitch);
        networkClient.sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, sendSoundPacket);
    }

    @SuppressWarnings("all")
    public void tryToPlaySoundToPlayer(@NotNull UUID playerId, @NotNull Sound sound, float volume, float pitch) {
        final var sendSoundPacket = new SendSoundToPlayerPacket(playerId, sound.getKey().toString(), volume, pitch);
        networkClient.sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, sendSoundPacket);
    }

    public SubscriptionCategoryType getSubscriptionCategoryType() {
        return SubscriptionCategoryType.GLOBAL;
    }

    public boolean isRunningOnNewVersions() {
        return PacketEvents.getAPI()
          .getServerManager()
          .getVersion()
          .isNewerThanOrEquals(ServerVersion.V_1_12);
    }
}

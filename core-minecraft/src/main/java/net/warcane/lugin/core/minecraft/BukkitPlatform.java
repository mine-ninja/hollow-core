package net.warcane.lugin.core.minecraft;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.MinecraftServerPlatform;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.centralcart.CentralCart;
import net.warcane.lugin.core.minecraft.currency.CurrencyManager;
import net.warcane.lugin.core.minecraft.event.tick.AsyncServerTickEvent;
import net.warcane.lugin.core.minecraft.internal.command.InternalCommandManager;
import net.warcane.lugin.core.minecraft.internal.listener.InternalPacketListeners;
import net.warcane.lugin.core.minecraft.internal.listener.InternalPlayerListener;
import net.warcane.lugin.core.minecraft.internal.listener.PlayerGroupUpdatingListener;
import net.warcane.lugin.core.minecraft.internal.listener.StaffTrackingListener;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.nametag.LegacyNameTagResolver;
import net.warcane.lugin.core.minecraft.nametag.ModernNameTagResolver;
import net.warcane.lugin.core.minecraft.nametag.NameTagResolver;
import net.warcane.lugin.core.minecraft.permission.PermissionInjector;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.util.message.AdventureFormatters;
import net.warcane.lugin.core.minecraft.vanish.VanishManager;
import net.warcane.lugin.core.minecraft.whitelist.WhitelistService;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import net.warcane.lugin.core.network.packet.impl.player.SendSoundToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerConnectToSubCategoryPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.player.state.PlayerNetworkStateManager;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.ServerPlayers;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BukkitPlatform extends AbstractPlatform implements MinecraftServerPlatform {


    private static final ScheduledExecutorService SINGLE_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(
      runnable -> new Thread(runnable, "BukkitPlatform-SingleExecutor")
    );

    /**
     * Cria uma instância da plataforma Bukkit (Se não existir) throwable a registra no Bukkit ServicesManager.
     *
     * @param plugin       o plugin Bukkit associado a esta plataforma.
     * @return a instância de BukkitPlatform.
     */
    public static BukkitPlatform provide(@NotNull JavaPlugin plugin) {
        final var rawType = Property.getOrThrow("SERVER_TYPE");
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
    private final WhitelistService whitelistService;

    @Getter private NameTagResolver nameTagResolver;
    @Getter private CentralCart centralCart;

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

        this.whitelistService = new WhitelistService(this);

        this.centralCart = new CentralCart();
        this.centralCart.initSocket();
        
        final var usesModernTags = Property.getBoolean("USE_MODERN_TAGS", false);
        if (usesModernTags) {
            this.nameTagResolver = new ModernNameTagResolver(this);
        } else {
            this.nameTagResolver = new LegacyNameTagResolver(this);
        }

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

        log.info("Initializing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());

        networkClient.subscribeToChannels(channels);
        log.info("Bukkit Platform initialized with channels: {}", Arrays.toString(channels));

        final var serverRegisterPacket = new ServerRegisterPacket(this.getId(), serverCategoryType, hostAddress);
        networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, serverRegisterPacket);

        Bukkit.getPluginManager().registerEvents(new InternalPlayerListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerGroupUpdatingListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new StaffTrackingListener(), plugin);

        PunishManager.init(plugin, getExecutorService());
        AdventureFormatters.init();

        final var internalPackets = new InternalPacketListeners(this);
        internalPackets.setup();

        this.online = true;

        gameServerService.update(this.getGameServer().withOnlineStatus(true));
        
        log.info("Bukkit Platform is now online with ID: {}, Category: {} and SubCategory: {}", this.getId(), this.getServerCategoryType(), this.getServerSubCategoryType());
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateServerInfo, 20, 20 * 10);
        Bukkit.getConsoleSender().sendMessage("§aCarregando nomes de jogadores para o redis (para acesso rápido)");
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

    public WhitelistService getWhitelistService() {
        return whitelistService;
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
          .isNewerThanOrEquals(ServerVersion.V_1_8_8);
    }

    @NotNull
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    @NotNull
    public VanishManager getVanishManager() {
        return vanishManager;
    }

    @NotNull
    public InternalCommandManager getInternalCommandManager() {
        return internalCommandManager;
    }

    @NotNull
    public SimpleMenuManager getMenuManager() {
        return menuManager;
    }
}

package io.github.minehollow.minecraft;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.minehollow.minecraft.centralcart.CentralCart;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.currency.CurrencyManager;
import io.github.minehollow.minecraft.discord.DiscordService;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.gamerule.GameRuleManager;
import io.github.minehollow.minecraft.gamerule.listener.WorldLoadListener;
import io.github.minehollow.minecraft.internal.command.InternalCommandManager;
import io.github.minehollow.minecraft.internal.listener.InternalPacketListeners;
import io.github.minehollow.minecraft.internal.listener.InternalPlayerListener;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.message.AdventureFormatters;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.vanish.VanishManager;
import io.github.minehollow.minecraft.wallet.PlayerWalletBukkitService;
import io.github.minehollow.sdk.AbstractPlatform;
import io.github.minehollow.sdk.MinecraftServerPlatform;
import io.github.minehollow.sdk.Platform;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToServerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerConnectToSubCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerDirectPlayGameCategoryPacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendSoundToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerRegisterPacket;
import io.github.minehollow.sdk.network.packet.impl.server.ServerUnregisterPacket;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.ServerPlayers;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.server.type.ServerSubCategoryType;
import io.github.minehollow.sdk.util.address.HostAddress;
import io.github.minehollow.sdk.util.property.Property;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
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
    private final CurrencyManager currencyManager;
    private final VanishManager vanishManager;
    private final SimpleMenuManager menuManager;
    private final GameRuleManager gameRuleManager;
    private final PlayerWalletBukkitService playerWalletService;


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
        this.currencyManager = new CurrencyManager(this);
        this.vanishManager = new VanishManager(this);
        this.menuManager = new SimpleMenuManager(this);

        this.gameRuleManager = new GameRuleManager(this);
        this.playerWalletService = new PlayerWalletBukkitService(this);

        this.centralCart = new CentralCart();
        this.centralCart.initSocket();

        this.discordService = new DiscordService(this);

        currencyManager.registerCurrency(new Currency(
          "rankup_coins",
          "Coin",
          "$",
          "Coins",
          "money",
          List.of("coins", "coin", "bal"),
          true
        ));

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

        log.debug("Initializing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());

        networkClient.subscribeToChannels(channels);
        log.debug("Bukkit Platform initialized with channels: {}", Arrays.toString(channels));

        final var serverRegisterPacket = new ServerRegisterPacket(this.getId(), serverCategoryType, hostAddress);
        networkClient.sendNetworkPacket(NetworkChannel.SERVER_STATUS, serverRegisterPacket);

        Bukkit.getPluginManager().registerEvents(new InternalPlayerListener(this), plugin);

        ChatInput.init(plugin);


        AdventureFormatters.init();

        final var internalPackets = new InternalPacketListeners(this);
        internalPackets.setup();

        this.online = true;

        gameServerService.update(this.getGameServer().withOnlineStatus(true));
        log.debug("Bukkit Platform is now online with ID: {}, Category: {} and SubCategory: {}", this.getId(), this.getServerCategoryType(), this.getServerSubCategoryType());
        Tasks.runAsyncRepeating(this::updateServerInfo, 20, 20 * 10);
        Bukkit.getConsoleSender().sendMessage("§aCarregando nomes de jogadores para o redis (para acesso rápido)");
    }

    @Override
    public void close() {
        log.debug("Closing Bukkit Platform with category: {}", serverCategoryType.getDisplayName());
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

    public String getDisallowJoinMessage() {
        return Property.get("DISALLOW_JOIN_MESSAGE", "§cVocê não tem permissão para entrar neste servidor.");
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
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

    public boolean isRunningOnNewVersions() {
        return PacketEvents.getAPI()
          .getServerManager()
          .getVersion()
          .isNewerThanOrEquals(ServerVersion.V_1_12);
    }
}

package net.warcane.lugin.core.minigames;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.AbstractPlatform;
import net.warcane.lugin.core.MinecraftServerPlatform;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.minecraft.event.tick.AsyncServerTickEvent;
import net.warcane.lugin.core.minecraft.util.message.AdventureFormatters;
import net.warcane.lugin.core.minigames.internal.command.InternalCommandManager;
import net.warcane.lugin.core.minigames.internal.listeners.InternalListener;
import net.warcane.lugin.core.minigames.internal.packets.InternalPacketListeners;
import net.warcane.lugin.core.minigames.party.PartyService;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.server.ServerPlayers;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class MinigamesPlatform extends AbstractPlatform implements MinecraftServerPlatform {

    private static final ScheduledExecutorService SINGLE_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(
        runnable -> new Thread(runnable, "MinigamesPlatform-SingleExecutor")
    );

    private final boolean isMainServer;

    private final JavaPlugin plugin;
    @NotNull
    private final ServerCategoryType serverCategoryType;
    private final ServerSubCategoryType serverSubCategoryType;

    private final InternalCommandManager internalCommandManager;
    private final InternalPacketListeners internalPacketListeners;
    private final InternalListener internalListeners;

    private final PartyService partyService;

    public MinigamesPlatform(JavaPlugin plugin, @NotNull ServerCategoryType serverCategoryType) {
        super(HostAddress.localAddress((short) Bukkit.getPort()));

        this.isMainServer = Property.getBoolean("MAIN_SERVER", false);
        this.plugin = plugin;
        this.serverCategoryType = serverCategoryType;
        this.serverSubCategoryType = Property.getEnum("SERVER_SUB_CATEGORY", ServerSubCategoryType.class, ServerSubCategoryType.NONE);
        this.internalCommandManager = new InternalCommandManager(this);
        this.internalPacketListeners = new InternalPacketListeners(this);
        this.internalListeners = new InternalListener(this, plugin);
        this.partyService = new PartyService(this);
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

    @Override
    public void init(NetworkChannel... channels) {
        SINGLE_EXECUTOR_SERVICE.scheduleAtFixedRate(
                AsyncServerTickEvent::call,
                0, 50, TimeUnit.MILLISECONDS
        );

        log.info("Initializing Minigames Platform with category: {}", serverCategoryType.getDisplayName());

        internalCommandManager.registerInternalCommands();
        internalPacketListeners.internalPacketListeners();
        internalListeners.registerListeners();

        networkClient.subscribeToChannels(channels);

        log.info("Minigames Platform initialized with channels: {}", Arrays.toString(channels));

        AdventureFormatters.init();
    }

    @Override
    public void close() {

    }

    public static MinigamesPlatform provide(@NotNull JavaPlugin plugin) {
        final var rawType = Property.getOrThrow("SERVER_TYPE");
        final var categoryType = ServerCategoryType.fromName(rawType);
        if (!isInitialized()) {
            return new MinigamesPlatform(plugin, categoryType);
        }

        final var currentInstance = getInstance();
        if (currentInstance.getServerCategoryType() != categoryType) {
            log.warn("Attempted to provide a MinigamesPlatform with a different category type. Current: {}, Provided: {}",
                currentInstance.getServerCategoryType(), categoryType);
        }
        return currentInstance;
    }

    public static boolean isInitialized() {
        return Bukkit.getServicesManager().isProvidedFor(Platform.class);
    }

    @NotNull
    public static MinigamesPlatform getInstance() {
        final var platform = Bukkit.getServicesManager().load(Platform.class);
        if (platform instanceof MinigamesPlatform minigamesPlatform) {
            return minigamesPlatform;
        }
        throw new IllegalStateException("MinigamesPlatform is not initialized or not available.");
    }
}

package io.github.minehollow.sdk;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.minehollow.sdk.database.MongoDbConnector;
import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.sdk.network.NetworkClient;
import io.github.minehollow.sdk.player.discord.PlayerDiscordService;
import io.github.minehollow.sdk.player.wallet.WalletService;
import io.github.minehollow.sdk.server.GameServerService;
import io.github.minehollow.sdk.util.address.HostAddress;
import io.github.minehollow.sdk.util.property.Property;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class AbstractPlatform implements Platform {
    static {
        // gambiarra para carregar tudo do .env no sistema
        Dotenv.configure().systemProperties().load();
    }

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static AbstractPlatform instance;
    protected final HostAddress hostAddress;

    protected final RedisConnector redisConnector;
    protected final MongoDbConnector mongoDbConnector;
    protected final ExecutorService executorService;

    protected final NetworkClient networkClient;

    protected final GameServerService gameServerService;
    protected final WalletService walletService;
    protected final PlayerDiscordService playerDiscordService;

    public AbstractPlatform(HostAddress hostAddress) {
        instance = this;
        this.hostAddress = hostAddress;

        this.redisConnector = RedisConnector.getInstance();
        this.mongoDbConnector = MongoDbConnector.getInstance();
        this.executorService = ASYNC_EXECUTOR;

        this.networkClient = new NetworkClient(this, hostAddress, executorService);
        this.gameServerService = new GameServerService(redisConnector);
        this.walletService = new WalletService();
        this.playerDiscordService = new PlayerDiscordService(executorService);
    }

    protected void loadGroupPermissions() {
    }

    @Override
    public String getId() {
        final var fromPlatformEnv = Property.get("PLATFORM_ID");
        if (fromPlatformEnv != null && !fromPlatformEnv.isBlank()) {
            return fromPlatformEnv;
        }

        final var fromSystem = System.getenv("P_SERVER_UUID");
        if (fromSystem != null && !fromSystem.isBlank()) {
            // we get the first part before the hyphen
            return fromSystem.split("-")[0];
        }

        throw new IllegalStateException("Platform ID not defined in environment variables.");
    }

    public static Platform getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Platform instance has not been initialized yet.");
        }
        return instance;
    }

    @Override
    public GameServerService getGameServerService() {
        return gameServerService;
    }

    @Override
    public WalletService getWalletService() {
        return walletService;
    }

    @Override
    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    @Override
    public ExecutorService getExecutorService() {
        return ASYNC_EXECUTOR;
    }

    @Override
    public PlayerDiscordService getPlayerDiscordService() {
        return playerDiscordService;
    }
}

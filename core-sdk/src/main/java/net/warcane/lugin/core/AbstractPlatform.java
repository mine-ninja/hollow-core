package net.warcane.lugin.core;

import io.github.cdimascio.dotenv.Dotenv;
import net.warcane.lugin.core.account.PlayerAccountService;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.server.GameServerService;
import net.warcane.lugin.core.util.address.HostAddress;
import net.warcane.lugin.core.util.property.Property;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractPlatform implements Platform {

    // Simple executor service for async tasks (with 1 threads)
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();

    protected final HostAddress hostAddress;

    protected final RedisConnector redisConnector;
    protected final MongoDbConnector mongoDbConnector;
    protected final ExecutorService executorService;

    protected final GameServerService gameServerService;
    protected final PlayerAccountService playerAccountService;

    public AbstractPlatform(HostAddress hostAddress) {
        this.loadProperties();

        this.hostAddress = hostAddress;
        this.redisConnector = RedisConnector.getInstance();
        this.mongoDbConnector = MongoDbConnector.getInstance();
        this.executorService = ASYNC_EXECUTOR;

        this.gameServerService = new GameServerService(redisConnector);
        this.playerAccountService = PlayerAccountService.createNewInstance(executorService);
    }

    @Override
    public String getId() {
        return Property.getOrThrow("PLATFORM_ID");
    }

    @Override
    public GameServerService getGameServerService() {
        return gameServerService;
    }

    @Override
    public PlayerAccountService getPlayerAccountService() {
        return playerAccountService;
    }

    private void loadProperties() {
        Dotenv.configure()
          .systemProperties()
          .load();
    }
}

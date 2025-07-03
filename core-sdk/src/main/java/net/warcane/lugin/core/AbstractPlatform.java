package net.warcane.lugin.core;

import io.github.cdimascio.dotenv.Dotenv;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.server.GameServerService;
import net.warcane.lugin.core.util.property.Property;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractPlatform implements Platform {

    // Simple executor service for async tasks (with 1 threads)
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();

    protected final RedisConnector redisConnector;
    protected final ExecutorService executorService;

    protected final GameServerService gameServerService;

    public AbstractPlatform(RedisConnector redisConnector) {
        this.loadProperties();

        this.redisConnector = redisConnector;
        this.executorService = ASYNC_EXECUTOR;
        this.gameServerService = new GameServerService(redisConnector);
    }

    @Override
    public String getId() {
        return Property.getOrThrow("PLATFORM_ID");
    }

    @Override
    public GameServerService getGameServerService() {
        return gameServerService;
    }

    private void loadProperties() {
        Dotenv.configure()
          .systemProperties()
          .load();
    }


}

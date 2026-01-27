package io.github.minehollow.sdk;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.minehollow.sdk.database.MongoDbConnector;
import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.sdk.group.GroupPermissionService;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.sdk.network.NetworkClient;
import io.github.minehollow.sdk.player.account.PlayerAccountService;
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
    
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()), r -> {
        Thread t = new Thread(r);
        t.setName("wallet-async-" + t.getId());
        t.setDaemon(true);
        return t;
    });
    
    private static AbstractPlatform instance;
    protected final HostAddress hostAddress;
    
    protected final RedisConnector redisConnector;
    protected final MongoDbConnector mongoDbConnector;
    protected final ExecutorService executorService;
    
    protected final NetworkClient networkClient;
    
    protected final GameServerService gameServerService;
    protected final PlayerAccountService playerAccountService;
    protected final GroupPermissionService groupPermissionService;
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
        this.playerAccountService = PlayerAccountService.of(executorService);
        this.groupPermissionService = new GroupPermissionService(executorService);
        this.walletService = new WalletService(this, executorService);
        this.playerDiscordService = new PlayerDiscordService(executorService);
    }
    
    protected void loadGroupPermissions() {
        for (PlayerGroup group : PlayerGroup.BY_ID.values()) {
            groupPermissionService.loadPermissions(group, true).whenComplete((found, error) -> {
                if (error != null) {
                    log.error("Erro ao carregar permissões do grupo {}: {}", group.name(), error.getMessage(), error);
                } else if (found == null) {
                    throw new IllegalStateException("Permissões do grupo " + group.name() + " não encontradas, verifique se o grupo foi criado corretamente.");
                } else {
                    log.info("Permissões do grupo {} carregadas com sucesso.", group.name());
                }
            });
        }
    }
    
    @Override
    public String getId() {
        return Property.getOrThrow("PLATFORM_ID", () -> new IllegalStateException("Propriedade 'PLATFORM_ID' não definida verifique seu arquivo '.env'"));
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
    public PlayerAccountService getPlayerAccountService() {
        return playerAccountService;
    }
    
    @Override
    public GroupPermissionService getGroupPermissionService() {
        return groupPermissionService;
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

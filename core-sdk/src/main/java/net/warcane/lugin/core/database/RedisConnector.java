package net.warcane.lugin.core.database;

import lombok.Data;
import net.warcane.lugin.core.util.property.Property;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
public class RedisConnector {

    private static RedisConnector instance;

    /**
     * Obtém a instância singleton do RedisConnector.
     *
     * @return Instância do RedisConnector.
     */
    public static @NotNull RedisConnector getInstance() {
        if (instance == null) {
            instance = fromInternalProperties();
        }
        return instance;
    }

    public static @NotNull RedisConnector fromInternalProperties() {
        final var redisUrl = Property.getOrThrow("REDIS_URL");
        return new RedisConnector(redisUrl);
    }

    public static @NotNull RedisConnector fromUrl(@NotNull String redisUri) {
        return new RedisConnector(redisUri);
    }

    private JedisPool jedisPool;
    private final String redisUri;

    public RedisConnector(String redisUri) {
        this.redisUri = redisUri;
        initializePool();
    }

    private void initializePool() {
        try {
            URI uri = new URI(redisUri);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 6379;
            String password =
              uri.getUserInfo() != null && uri.getUserInfo().contains(":") ? uri.getUserInfo().split(":", 2)[1] : null;

            JedisPoolConfig poolConfig = new JedisPoolConfig();

            poolConfig.setMaxTotal(32);
            poolConfig.setMaxIdle(32);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);

            /*jedisPool = new JedisPool(poolConfig, new HostAndPort(host, port), DefaultJedisClientConfig.builder()
                .socketTimeoutMillis(2000)
                .connectionTimeoutMillis(2000)
                .blockingSocketTimeoutMillis(2000)
                .password(password)
                .build());*/
            jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Redis URI: " + redisUri, e);
        }
    }

    public <T> T supplyFromJedis(@NotNull Function<Jedis, T> jedisFunction) {
        try (var jedisInstance = jedisPool.getResource()) {
            return jedisFunction.apply(jedisInstance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute Jedis function", e);
        }
    }

    public void useJedis(@NotNull Consumer<Jedis> jedis) {
        try (Jedis jedisInstance = jedisPool.getResource()) {
            jedis.accept(jedisInstance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to use Jedis instance", e);
        }
    }
}

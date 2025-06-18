package net.warcane.lugin.core.database;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.Data;

@Data
public class RedisConnector {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    public RedisConnector(String redisUri) {
        this.redisClient = RedisClient.create(redisUri);
        this.connection = redisClient.connect();
    }

    public RedisCommands<String, String> getCommands() {
        return connection.sync();
    }

    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}
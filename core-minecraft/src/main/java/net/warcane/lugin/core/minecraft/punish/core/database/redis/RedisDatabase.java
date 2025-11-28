package net.warcane.lugin.core.minecraft.punish.core.database.redis;

import lombok.Getter;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 */
@Getter
public class RedisDatabase {

    private static RedisDatabase instance;

    public void subscribe(Plugin plugin, JedisPubSub jedisPubSub, String... channels) {
        if (Tasks.isFolia()) {
            Tasks.runAsync(() -> {
                RedisConnector.getInstance().useJedis(jedis -> {
                    jedis.subscribe(jedisPubSub, channels);
                });
            });
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RedisConnector.getInstance().useJedis(jedis -> {
                jedis.subscribe(jedisPubSub, channels);
            });
        });
    }

    public void publish(PubSubMessage message) {
        RedisConnector.getInstance().useJedis(jedis -> {
            jedis.publish(message.getChannel(), message.serialize());
        });
    }

    public static void init() {
        if (instance == null) {
            instance = new RedisDatabase();
            return;
        }
        throw new IllegalStateException("RedisDatabase is already initialized.");
    }


    public static RedisDatabase get() {
        if (instance == null) {
            throw new IllegalStateException("RedisDatabase is not initialized. Call RedisDatabase.init(host, port) first.");
        }
        return instance;
    }
}

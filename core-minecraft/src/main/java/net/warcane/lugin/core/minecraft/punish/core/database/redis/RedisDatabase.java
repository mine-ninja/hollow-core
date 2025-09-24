package net.warcane.lugin.core.minecraft.punish.core.database.redis;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 * @project punish
 */
@Getter
public class RedisDatabase {

    private static RedisDatabase instance;

    protected JedisPool pool;

    private RedisDatabase(String host) {
        pool = new JedisPool(host);
    }

    public void subscribe(Plugin plugin, JedisPubSub jedisPubSub, String... channels) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(jedisPubSub, channels);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to subscribe to Redis channels: " + e.getMessage());
            }
        });
    }

    public void publish(PubSubMessage message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(message.getChannel(), message.serialize());
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to publish message to Redis channel: " + e.getMessage());
        }
    }

    public static void init() {
        if (instance == null) {
            instance = new RedisDatabase(System.getProperty("REDIS_URL"));
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

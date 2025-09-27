package net.warcane.lugin.core.minecraft.punish.core.database.redis;

import net.warcane.lugin.core.minecraft.punish.api.message.PunishMessagePubSub;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 */
public class MessageManager extends JedisPubSub {

    private static MessageManager instance;

    public static final String PUNISH_MESSAGE_CHANNEL = "punish_message";

    private MessageManager() {}

    public static void init(Plugin plugin) {
        if (instance == null) {
            instance = new MessageManager();
        } else {
            throw new IllegalStateException("MessageManager is already initialized.");
        }
        RedisDatabase.get().subscribe(plugin, new MessageManager(), PUNISH_MESSAGE_CHANNEL);
    }

    public void sendMessage(PubSubMessage message) {
        RedisDatabase.get().publish(message);
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(PUNISH_MESSAGE_CHANNEL)) {
            PubSubMessage.deserializeMessage(message, PunishMessagePubSub.class).handle();
        }
    }

    public static MessageManager get() {
        if (instance == null) {
            throw new IllegalStateException("MessageManager is not initialized. Call MessageManager.init(plugin) first.");
        }
        return instance;
    }
}

package net.warcane.lugin.core.minecraft.punish.core.database.redis;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 */
public interface PubSubMessage {

    String serialize();

    String getChannel();

    void handle();

    public static <T extends PubSubMessage> T deserializeMessage(String message, Class<T> type) {
        try {
            return type.getDeclaredConstructor(String.class).newInstance(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize message: " + message, e);
        }
    }
}

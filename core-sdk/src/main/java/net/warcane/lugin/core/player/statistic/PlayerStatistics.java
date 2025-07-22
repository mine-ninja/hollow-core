package net.warcane.lugin.core.player.statistic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import lombok.Getter;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class PlayerStatistics {

    public static final String REDIS_PREFIX = "playerstat:";

    private final Jedis jedisPool;
    private final MongoCollection<Document> collection;
    @Getter
    private final HashMap<String, HashMap<Integer, Integer>> cache;
    private final UUID uuid;

    private PlayerStatistics(@NotNull UUID uuid) {
        this.jedisPool = RedisConnector.getInstance().getJedisPool().getResource();
        this.collection = MongoDbConnector.getInstance().getCollection("stats", Document.class);

        this.cache = new HashMap<>();
        this.uuid = uuid;
    }

    /**
     * Cria uma classe sem estatística nenhuma.
     *
     * @param uniqueId O ID único do jogador
     * @return Uma nova instância de PlayerStatistics sem elementos.
     */
    @NotNull
    public static PlayerStatistics createBlankCache(@NotNull UUID uniqueId) {
        return new PlayerStatistics(uniqueId);
    }

    /**
     * Adiciona um valor à uma key das estatísticas.
     */
    public void addValue(int day, @NotNull String key, int value) {
        setValue(day, key, getValue(key, day) + value);
    }

    /**
     * Remove um valor à uma key das estatísticas.
     */
    public void removeValue(int day, @NotNull String key, int value) {
        setValue(day, key, Math.max(getValue(key, day) - value, 0));
    }

    /**
     * Define um valor à uma key das estatísticas.
     */
    public void setValue(int day, @NotNull String key, int value) {
        cache.computeIfAbsent(key, k -> new HashMap<>()).put(day, value);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteStream);

        Set<Map.Entry<Integer, Integer>> entrySet = cache.get(key).entrySet();

        try {
            dataOut.writeInt(entrySet.size());

            for (Map.Entry<Integer, Integer> entry : entrySet) {
                dataOut.writeInt(entry.getKey());
                dataOut.writeInt(entry.getValue());
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }

        byte[] bytes = byteStream.toByteArray();
        String encoded = java.util.Base64.getEncoder().encodeToString(bytes);

        if (jedisPool.exists(REDIS_PREFIX + uuid)) {
            jedisPool.hset(REDIS_PREFIX + uuid, key, encoded);
        }

        Document searchQuery = new Document("key", uuid.toString());
        Document document = collection.find(searchQuery).first();

        if (document != null) {
            if (document.getString(key) != null)
                return;

            document.put(key, encoded);

            collection.updateOne(searchQuery, new Document("$set", document), new UpdateOptions().upsert(true));
        } else {
            Document newDocument = new Document("key", uuid.toString()).append(key, encoded);
            collection.insertOne(newDocument);
        }
    }

    /**
     * Pega o valor à uma key das estatísticas pelos dias.
     */
    public int getValue(@NotNull String key, int... days) {
        int totalValue = 0;

        for (int day : days)
            totalValue += cache.containsKey(key) ? cache.get(key).getOrDefault(day, 0) : 0;

        return totalValue;
    }

    /**
     * Pega o valor à uma key das redis totalmente.
     */
    public int getTotalValue(@NotNull String key) {
        if (!cache.containsKey(key))
            return 0;

        Collection<Integer> values = cache.get(key).values();

        int totalValue = 0;

        for (int integer : values)
            totalValue += integer;

        return totalValue;
    }
}

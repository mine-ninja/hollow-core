package net.warcane.lugin.core.minigames.statistic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class PlayerStatistics {

    public static final String REDIS_PREFIX = "playerstat:";

    private static final RedisConnector redisConnector = RedisConnector.getInstance();
    private static final MongoCollection<Document> collection = MongoDbConnector.getInstance().getCollection("stats", Document.class);

    @Getter
    private final HashMap<String, HashMap<Integer, Integer>> cache;
    private final UUID uuid;

    private final ExecutorService executorService;

    private PlayerStatistics(@NotNull UUID uuid, ExecutorService executorService) {
        this.cache = new HashMap<>();
        this.uuid = uuid;

        this.executorService = executorService;
    }

    /**
     * Cria uma classe sem estatística nenhuma.
     *
     * @param uniqueId O ID único do jogador
     * @return Uma nova instância de PlayerStatistics sem elementos.
     */
    @NotNull
    public static PlayerStatistics createBlankCache(@NotNull UUID uniqueId, @NotNull ExecutorService executorService) {
        return new PlayerStatistics(uniqueId, executorService);
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
        executorService.submit(() -> {
            cache.computeIfAbsent(key, k -> new HashMap<>()).put(day, value);

            String redisKey = REDIS_PREFIX + uuid + ":" + key;

            redisConnector.useJedis(jedis -> jedis.hset(redisKey, String.valueOf(day), String.valueOf(value)));

            Document dayValueDoc = new Document("day", day).append("value", value);

            Bson filter = Filters.and(
                Filters.eq("key", uuid),
                Filters.elemMatch(key, Filters.eq("day", day))
            );

            Bson update = Updates.set(key + ".$", dayValueDoc);
            UpdateResult result = collection.updateOne(filter, update);

            if (result.getModifiedCount() == 0) {
                Bson removeOldEntry = Updates.pull(key, new Document("day", day));
                collection.updateOne(Filters.eq("key", uuid), removeOldEntry);

                Bson pushNewEntry = Updates.push(key, dayValueDoc);

                collection.updateOne(Filters.eq("key", uuid), pushNewEntry, new UpdateOptions().upsert(true));
            }
        });
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

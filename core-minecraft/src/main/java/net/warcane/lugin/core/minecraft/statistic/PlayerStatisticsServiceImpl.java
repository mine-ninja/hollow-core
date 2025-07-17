package net.warcane.lugin.core.minecraft.statistic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class PlayerStatisticsServiceImpl implements PlayerStatisticsService {

    private final Jedis jedisPool;
    private final MongoCollection<Document> collection;

    private final Map<UUID, PlayerStatistics> localCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public PlayerStatisticsServiceImpl(@NotNull ExecutorService executorService) {
        this.executorService = executorService;

        this.jedisPool = RedisConnector.getInstance().getJedisPool().getResource();
        this.collection = MongoDbConnector.getInstance().getCollection("stats", Document.class);

        collection.createIndex(Indexes.hashed("key"));
    }

    @Override
    public @Nullable PlayerStatistics getCachedAccount(@NotNull UUID playerId) {
        return localCache.get(playerId);
    }

    @Override
    public CompletableFuture<@Nullable PlayerStatistics> getPlayerAccount(@NotNull UUID playerId) {
        PlayerStatistics locallyCached = localCache.get(playerId);

        if (locallyCached != null)
            return CompletableFuture.completedFuture(locallyCached);

        return loadPlayerAccount(playerId);
    }

    @Override
    public CompletableFuture<@NotNull PlayerStatistics> loadPlayerAccount(@NotNull UUID playerId) {
        return supply(() -> {
            PlayerStatistics playerStatistics = PlayerStatistics.createBlankCache(playerId);

            if (jedisPool.exists(PlayerStatistics.REDIS_PREFIX + playerId)) {
                Map<String, String> hashMap = jedisPool.hgetAll(PlayerStatistics.REDIS_PREFIX + playerId);

                for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                    String encoded = entry.getValue();
                    byte[] bytes = encoded.getBytes(StandardCharsets.UTF_8);

                    DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(bytes));

                    try {
                        int amount = dataIn.readInt();

                        for (int i = 0; i <= amount; i++) {
                            int day = dataIn.readInt();
                            int value = dataIn.readInt();

                            playerStatistics.getCache().computeIfAbsent(entry.getKey(), k -> new HashMap<>()).put(day, value);
                        }
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }

                localCache.put(playerId, playerStatistics);
                return playerStatistics;
            }

            Document searchQuery = new Document("key", playerId.toString());
            Document document = collection.find(searchQuery).first();

            if (document != null) {
                for (Map.Entry<String, Object> entry : document.entrySet()) {
                    String encoded = (String) entry.getValue();
                    byte[] bytes = encoded.getBytes(StandardCharsets.UTF_8);

                    DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(bytes));

                    jedisPool.hset(PlayerStatistics.REDIS_PREFIX + playerId, entry.getKey(), encoded);

                    try {
                        int amount = dataIn.readInt();

                        for (int i = 0; i <= amount; i++) {
                            int day = dataIn.readInt();
                            int value = dataIn.readInt();

                            playerStatistics.getCache().computeIfAbsent(entry.getKey(), k -> new HashMap<>()).put(day, value);
                        }
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }

            localCache.put(playerId, playerStatistics);
            return playerStatistics;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerStatistics> unloadPlayerAccount(@NotNull UUID playerId) {
        final var removedFromLocalCache = localCache.remove(playerId);

        if (removedFromLocalCache == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Failed to unload player statistics: " + playerId));
        }

        return supply(() -> {
            jedisPool.del(PlayerStatistics.REDIS_PREFIX + playerId);
            return removedFromLocalCache;
        });
    }

    private <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }
}

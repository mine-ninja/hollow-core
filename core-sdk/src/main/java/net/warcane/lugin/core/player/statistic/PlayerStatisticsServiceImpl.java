package net.warcane.lugin.core.player.statistic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Slf4j
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

            Document searchQuery = new Document("key", playerId);
            Document document = collection.find(searchQuery).first();

            if (document != null) {
                for (Map.Entry<String, Object> entry : document.entrySet()) {
                    if (entry.getValue() instanceof ArrayList) {
                        String statKey = entry.getKey();

                        try {
                            @SuppressWarnings("unchecked")
                            ArrayList<Document> statsList = (ArrayList<Document>) entry.getValue();

                            for (Document dailyStat : statsList) {
                                int day = dailyStat.getInteger("day");
                                int value = dailyStat.getInteger("value");

                                playerStatistics.getCache().computeIfAbsent(statKey, k -> new HashMap<>()).put(day, value);
                            }
                        } catch (ClassCastException exception) {
                            log.error("Data format error for key '{}'. Expected an array of documents. {}", statKey, exception.getMessage());
                        }
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

        if (removedFromLocalCache == null)
            return CompletableFuture.failedFuture(new IllegalStateException("Failed to unload player statistics: " + playerId));

        return supply(() -> {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams().match(PlayerStatistics.REDIS_PREFIX + playerId + ":*").count(100);

            do {
                ScanResult<String> scanResult = jedisPool.scan(cursor, scanParams);
                cursor = scanResult.getCursor();

                if (!scanResult.getResult().isEmpty()) {
                    jedisPool.del(scanResult.getResult().toArray(new String[0]));
                }
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));

            return removedFromLocalCache;
        });
    }

    private <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }
}

package net.warcane.lugin.core.minigames.statistic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Slf4j
public class PlayerStatisticsServiceImpl implements PlayerStatisticsService {

    private static PlayerStatisticsService instance = null;

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

    public static PlayerStatisticsService getInstance() {
        if (instance == null)
            instance = new PlayerStatisticsServiceImpl(BukkitPlatform.getInstance().getExecutorService());

        return instance;
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
            PlayerStatistics playerStatistics = PlayerStatistics.createBlankCache(playerId, executorService);

            Document document = collection.find(new Document("key", playerId)).first();

            if (document != null) {
                document.remove("_id");
                document.remove("key");

                for (Map.Entry<String, Object> entry : document.entrySet()) {
                    String statKey = entry.getKey();
                    Object value = entry.getValue();

                    List<?> rawList = (List<?>) value;

                    if (rawList.isEmpty() || !(rawList.getFirst() instanceof Document)) {
                        log.warn("Skipping stat key '{}': expected a list of documents, but found a list of {}.", statKey, rawList.isEmpty() ? "nothing" : rawList.getFirst().getClass().getSimpleName());
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    List<Document> statsList = (List<Document>) rawList;

                    for (Document dailyStat : statsList) {
                        try {
                            Integer day = dailyStat.getInteger("day");
                            Integer statValue = dailyStat.getInteger("value");

                            if (day != null && statValue != null) {
                                playerStatistics.getCache().computeIfAbsent(statKey, k -> new HashMap<>()).put(day, statValue);
                            } else {
                                log.warn("Skipping daily stat for key '{}' due to missing 'day' or 'value'.", statKey);
                            }
                        } catch (Exception exception) {
                            log.error("Failed to parse daily stat document for key '{}': {}", statKey, dailyStat.toJson(), exception);
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

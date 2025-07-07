package net.warcane.lugin.core.player.account;

import com.mongodb.client.model.Indexes;
import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class PlayerAccountServiceImpl implements PlayerAccountService {

    private static final String CACHE_KEY = "playeracc";

    private final Map<UUID, PlayerAccount> localCache = new ConcurrentHashMap<>();
    private final RedisCache<PlayerAccount> redisCache = new RedisCache<>(PlayerAccount.class);

    private final MongoRepository<UUID, PlayerAccount> repository = new MongoRepository<>(PlayerAccount.class, "uniqueId");

    private final ExecutorService executorService;

    public PlayerAccountServiceImpl(@NotNull ExecutorService executorService) {
        this.executorService = executorService;

        repository.useCollection(collection -> collection.createIndex(Indexes.hashed("uniqueId")));
    }

    @Override
    public @Nullable PlayerAccount getCachedAccount(@NotNull UUID playerId) {
        return localCache.get(playerId);
    }

    @Override
    public CompletableFuture<@Nullable PlayerAccount> getPlayerAccount(@NotNull UUID playerId) {
        final var locallyCached = localCache.get(playerId);
        if (locallyCached != null) return CompletableFuture.completedFuture(locallyCached);

        return supply(() -> {
            final var fromRedis = redisCache.hget(CACHE_KEY, playerId.toString(),
              () -> repository.findById(playerId));
            if (fromRedis != null) {
                localCache.put(playerId, fromRedis);
            }
            return fromRedis;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerAccount> updatePlayerAccount(@NotNull PlayerAccount toUpdate, @NotNull AccountUpdateOptions options) {
        return supply(() -> {
            final var updated = repository.save(toUpdate, PlayerAccount::uniqueId);
            if (updated == null) {
                throw new IllegalStateException("Failed to update player account: " + toUpdate.uniqueId());
            }

            if (options.updateCaches()) {
                redisCache.hset(CACHE_KEY, toUpdate.uniqueId().toString(), updated);
                localCache.put(toUpdate.uniqueId(), updated);
            }
            return updated;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerAccount> loadPlayerAccount(@NotNull UUID playerId, @NotNull AccountLoadOptions options) {
        return supply(() -> {
            final var fromDb = repository.findById(playerId, options::getAccountOrThrow);
            if (options.cacheResult()) {
                redisCache.hset(CACHE_KEY, playerId.toString(), fromDb);
                localCache.put(playerId, fromDb);
            }
            return fromDb;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerAccount> unloadPlayerAccount(@NotNull UUID playerId, @NotNull AccountUnloadOptions options) {
        final var removedFromLocalCache = localCache.remove(playerId);
        if (removedFromLocalCache == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Failed to unload player account: " + playerId));
        }

        return supply(() -> {
            var removed = options.updateBeforeUnload()
              ? repository.save(removedFromLocalCache, PlayerAccount::uniqueId)
              : removedFromLocalCache;

            if (options.unloadFromCache()) {
                redisCache.hdel(CACHE_KEY, playerId.toString());
            }
            return removed;
        });
    }

    private <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }
}

package net.warcane.lugin.core.player.account;

import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.player.fetcher.PlayerUuidFetcher;
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

@Slf4j
public class PlayerAccountServiceImpl implements PlayerAccountService {

    private static final String CACHE_KEY = "playeracc";

    private final Map<UUID, PlayerAccount> localCache = new ConcurrentHashMap<>();
    private final RedisCache<PlayerAccount> redisCache = new RedisCache<>(PlayerAccount.class);

    private final MongoRepository<UUID, PlayerAccount> repository = new MongoRepository<>(PlayerAccount.class, "uniqueId");

    private final ExecutorService executorService;

    public PlayerAccountServiceImpl(@NotNull ExecutorService executorService) {
        this.executorService = executorService;

        repository.useCollection(collection -> {
            collection.createIndex(Indexes.hashed("uniqueId"));
            collection.createIndex(Indexes.hashed("playerName"));
        });
    }

    @Override
    public @Nullable PlayerAccount getCachedAccount(@NotNull UUID playerId) {
        return localCache.get(playerId);
    }

    @Override
    public @Nullable PlayerAccount getCachedAccountByName(@NotNull String playerName) {
        return localCache.values()
          .stream()
          .filter(account -> account.playerName().equalsIgnoreCase(playerName))
          .findFirst()
          .orElse(null);
    }

    @Override
    public CompletableFuture<@Nullable PlayerAccount> getPlayerAccount(@NotNull UUID playerId) {
        final var locallyCached = localCache.get(playerId);
        if (locallyCached != null) return CompletableFuture.completedFuture(locallyCached);

        return supply(() -> {
            final var fromRedis = redisCache.hget(CACHE_KEY, playerId.toString(), () -> repository.findById(playerId));
            if (fromRedis != null) {
                localCache.put(playerId, fromRedis);
            }
            return fromRedis;
        });
    }

    @Override
    public CompletableFuture<@Nullable PlayerAccount> getPlayerAccountByName(@NotNull String playerName) {
        final var local = getCachedAccountByName(playerName);
        if (local != null) {
            log.info("Found player account for {} in local cache.", playerName);
            return CompletableFuture.completedFuture(local);
        }

        final var playerId = PlayerUuidFetcher.getInstance().fetchPlayerUuid(playerName);
        if (playerId != null) {
            log.info("Fetched UUID for player {}: {}", playerName, playerId);
            return getPlayerAccount(playerId);
        }

        return supply(() -> {


            final var fromMongo = repository.findFirstFromPropertyIgnoreCase("playerName", playerName);
            if (fromMongo != null) {
                log.info("Found player account for {} in MongoDB: {}", playerName, fromMongo);
                localCache.put(fromMongo.uniqueId(), fromMongo);
                redisCache.hset(CACHE_KEY, fromMongo.uniqueId().toString(), fromMongo);
                return fromMongo;
            }

            log.info("Player account for {} not found in local cache or MongoDB, fetching UUID.", playerName);
            return null;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerAccount> updatePlayerAccount(@NotNull PlayerAccount toUpdate, @NotNull AccountUpdateOptions options) {
        return supply(() -> {
            log.info("Updating player account for {}: {}", toUpdate.uniqueId(), toUpdate);

            final var updated = repository.save(toUpdate, PlayerAccount::uniqueId);
            if (updated == null) {
                throw new IllegalStateException("Failed to update player account: " + toUpdate.uniqueId());
            }


            if (options.updateCaches()) {
                log.info("Updating caches for player account: {}", toUpdate.uniqueId());
                redisCache.hset(CACHE_KEY, toUpdate.uniqueId().toString(), updated);
                localCache.put(toUpdate.uniqueId(), updated);
            }

            return updated;
        });
    }

    @Override
    public CompletableFuture<@NotNull PlayerAccount> loadPlayerAccount(@NotNull UUID playerId, @NotNull AccountLoadOptions options) {
        return supply(() -> {
            var fromDb = repository.findById(playerId);
            if (fromDb == null) {
                if (!options.hasAccountCreator()) {
                    throw new IllegalStateException("Failed to load player account: " + playerId);
                }

                fromDb = options.getAccountOrThrow();
            }

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

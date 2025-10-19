package net.warcane.lugin.core.player.discord;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PlayerDiscordService implements IPlayerDiscordService {

    private static final String CACHE_KEY = "playerdiscord";
    private final RedisCache<PlayerDiscord> redisCache = new RedisCache<>(PlayerDiscord.class);
    private final MongoRepository<UUID, PlayerDiscord> repository = new MongoRepository<>(PlayerDiscord.class, "playerId");

    @NotNull
    private final ExecutorService executorService;

    public PlayerDiscordService(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
        repository.removeDuplicates("playerId");
    }

    @Override
    public @Nullable PlayerDiscord loadFromRedis(@NotNull UUID playerId) {
        return redisCache.hget(CACHE_KEY, playerId.toString());
    }

    @Override
    public void updateCaches(@NotNull PlayerDiscord playerDiscord) {
        var playerId = playerDiscord.playerId();
        redisCache.hset(CACHE_KEY, playerId.toString(), playerDiscord);
    }

    @Override
    public CompletableFuture<@Nullable PlayerDiscord> getPlayerDiscord(@NotNull UUID playerId) {
        return CompletableFuture
            .supplyAsync(() -> redisCache.hget(CACHE_KEY, playerId.toString(), () -> repository.findById(playerId)), executorService);
    }

    @Override
    public CompletableFuture<@NotNull PlayerDiscord> updatePlayerDiscord(@NotNull PlayerDiscord toUpdate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Updating player discord for {}: {}", toUpdate.playerId(), toUpdate);

                final var updated = repository.save(toUpdate, PlayerDiscord::playerId);
                if (updated == null) {
                    throw new IllegalStateException("Failed to update player settings: " + toUpdate.playerId());
                }

                redisCache.hset(CACHE_KEY, updated.playerId().toString(), updated);

                return updated;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to update player discord: " + toUpdate.playerId(), exception);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<@NotNull PlayerDiscord> loadPlayerDiscord(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            var fromDb = repository.findById(playerId);
            if (fromDb == null) {
                fromDb = PlayerDiscord.createDefaultSettings(playerId);
                repository.save(fromDb, PlayerDiscord::playerId);
            }

            redisCache.hset(CACHE_KEY, playerId.toString(), fromDb);

            return fromDb;
        }, executorService);
    }

    @Override
    public void unloadPlayerDiscord(@NotNull UUID playerId) {
        redisCache.hdel(CACHE_KEY, playerId.toString());
    }
}

package net.warcane.lugin.core.player.fetcher;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.util.data.RedisCache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching and caching player profiles.
 * <p>
 * Uses a three-layer cache strategy:
 * <ul>
 *   <li>Layer 1: Caffeine in-memory cache (5 minutes TTL)</li>
 *   <li>Layer 2: Redis cache (7 days TTL)</li>
 *   <li>Layer 3: Database via PlayerAccountService</li>
 * </ul>
 * <p>
 * All operations are non-blocking and return CompletableFuture.
 */
public class ProfileService {
    private static final long REDIS_TTL = 604_800L;
    
    private final Platform platform;
    private final AsyncLoadingCache<String, SimpleProfile> cache;
    private final RedisCache<SimpleProfile> profileCache = new RedisCache<>(SimpleProfile.class);
    
    public ProfileService(Platform platform) {
        this.platform = platform;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .buildAsync((key, executor) -> {
                SimpleProfile cached = getCachedProfile(key);
                if (cached != null) {
                    return CompletableFuture.completedFuture(cached);
                }
                
                try {
                    UUID uuid = UUID.fromString(key);
                    return this.platform.getPlayerAccountService().getPlayerAccount(uuid)
                        .thenApply(account -> {
                            if (account == null) {
                                return null;
                            }
                            SimpleProfile profile = new SimpleProfile(
                                account.uniqueId(),
                                account.playerName(),
                                account.skin()
                            );
                            cacheProfile(profile);
                            return profile;
                        });
                } catch (IllegalArgumentException e) {
                    return this.platform.getPlayerAccountService().getPlayerAccountByName(key)
                        .thenApply(account -> {
                            if (account == null) {
                                return null;
                            }
                            SimpleProfile profile = new SimpleProfile(
                                account.uniqueId(),
                                account.playerName(),
                                account.skin()
                            );
                            cacheProfile(profile);
                            return profile;
                        });
                }
            });
    }
    
    /**
     * Gets a profile by identifier (UUID or name) asynchronously.
     * <p>
     * This method is non-blocking and returns immediately with a CompletableFuture.
     * <p>
     * Example usage:
     * <pre>{@code
     * profileService.getProfileAsync("player123").thenAccept(profile -> {
     *     if (profile != null) {
     *         // use profile
     *     }
     * });
     * }</pre>
     *
     * @param identifier the UUID or name
     * @return CompletableFuture with the profile, or null if not found
     */
    public CompletableFuture<SimpleProfile> getProfileAsync(String identifier) {
        return this.cache.get(identifier);
    }
    
    /**
     * Gets a profile by identifier (UUID or name) synchronously.
     * <p>
     * WARNING: This method BLOCKS the current thread until the profile is loaded.
     * Only use this when you absolutely need synchronous access and understand the performance implications.
     * <p>
     * Prefer using {@link #getProfileAsync(String)} for better performance.
     *
     * @param identifier the UUID or name
     * @return the profile, or null if not found
     * @deprecated Use {@link #getProfileAsync(String)} instead to avoid blocking
     */
    @Nullable
    @Deprecated
    public SimpleProfile getProfile(String identifier) {
        return this.cache.synchronous().get(identifier);
    }
    
    /**
     * Gets a cached profile synchronously from Redis.
     * <p>
     * This only checks the Redis cache, does not query the database.
     *
     * @param identifier the UUID or name
     * @return the profile, or null if not cached
     */
    @Nullable
    public SimpleProfile getCachedProfile(@NotNull String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return profileCache.get("profile:uuid:" + uuid);
        } catch (IllegalArgumentException e) {
            return profileCache.get("profile:name:" + identifier.toLowerCase());
        }
    }
    
    /**
     * Caches a profile in Redis with 7-day TTL.
     *
     * @param profile the profile to cache
     */
    public void cacheProfile(@NotNull SimpleProfile profile) {
        profileCache.set("profile:uuid:" + profile.getUuid(), profile, REDIS_TTL);
        profileCache.set("profile:name:" + profile.getName().toLowerCase(), profile, REDIS_TTL);
    }
    
    /**
     * Removes a profile from both Caffeine and Redis caches.
     *
     * @param profile the profile to remove
     */
    public void removeProfile(@NotNull SimpleProfile profile) {
        cache.synchronous().invalidate(profile.getUuid().toString());
        cache.synchronous().invalidate(profile.getName().toLowerCase());
        profileCache.del("profile:uuid:" + profile.getUuid());
        profileCache.del("profile:name:" + profile.getName().toLowerCase());
    }
}

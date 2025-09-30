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

public class ProfileService {
    private static final long TTL = 604_800L;
    
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
                    this.platform.getPlayerAccountService().getPlayerAccount(uuid)
                        .thenCompose(account -> {
                            if (account == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            SimpleProfile profile = new SimpleProfile(account.uniqueId(), account.playerName(), account.skin());
                            cacheProfile(profile);
                            return CompletableFuture.completedFuture(profile);
                        });
                } catch (IllegalArgumentException e) {
                    return this.platform.getPlayerAccountService().getPlayerAccountByName(key)
                       .thenCompose(account -> {
                           if (account == null) {
                               return CompletableFuture.completedFuture(null);
                           }
                           SimpleProfile profile = new SimpleProfile(account.uniqueId(), account.playerName(), account.skin());
                           cacheProfile(profile);
                           return CompletableFuture.completedFuture(profile);
                       });
                }
                
                return CompletableFuture.completedFuture(null);
            });
    }
    
    public CompletableFuture<@Nullable SimpleProfile> getProfile(String identifier) {
        return this.cache.get(identifier);
    }
    
    /**
     * Busca perfil por identificador (nome ou UUID)
     */
    @Nullable
    public SimpleProfile getCachedProfile(@NotNull String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return profileCache.get("profile:uuid:" + uuid);
        }
        catch (IllegalArgumentException e) {
            return profileCache.get("profile:name:" + identifier.toLowerCase());
        }
    }
    
    /**
     * Armazena perfil completo no cache (incluindo skin)
     */
    public void cacheProfile(@NotNull SimpleProfile profile) {
        profileCache.set("profile:uuid:" + profile.getUuid(), profile, TTL);
        profileCache.set("profile:name:" + profile.getName().toLowerCase(), profile, TTL);
    }
    
    /**
     * Remove perfil do cache
     */
    public void removeProfile(@NotNull SimpleProfile profile) {
        profileCache.del("profile:uuid:" + profile.getUuid());
        profileCache.del("profile:name:" + profile.getName().toLowerCase());
    }
}

/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.cache;

import gg.nerdzone.common.RedisService;
import gg.nerdzone.common.cache.LocalCacheParams;
import gg.nerdzone.common.cache.RedisCache;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.model.PrisonUserProfile;
import java.time.Duration;
import lombok.NonNull;
import me.lucko.helper.Services;
import org.jetbrains.annotations.Nullable;

public class MiningUserCache {

    public static @NonNull MiningUserCache create(@NonNull RedisService redisService) {
        return new MiningUserCache(redisService);
    }

    private final RedisCache<String, MiningUser> cache;

    private MiningUserCache(RedisService redisService) {
        if (Services.get(MiningUserCache.class).isPresent()) {
            throw new IllegalStateException("MiningUserCache is already initialized");
        }

        final LocalCacheParams<String, MiningUser> params = LocalCacheParams.<String, MiningUser>builder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .caseSensitiveKeys(false)
            .build();

        this.cache = RedisCache.newCache("mc.prison.mining:users", redisService, String.class, MiningUser.class, params);

        Services.provide(MiningUserCache.class, this);
    }

    public void insert(@NonNull MiningUser user) {
        this.cache.insert(user.getName(), user);
    }

    public @Nullable MiningUser find(@NonNull String username) {
        return this.cache.get(username);
    }

    public MiningUser findOrCreate(@NonNull String username, @NonNull PrisonUserProfile profile) {
        return this.cache.getOrCreate(username, key -> new MiningUser(profile.getUsername()));
    }
}

/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.nerdzone.common.RedisService;
import gg.nerdzone.common.cache.RedisCache;
import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.service.PrisonUserProfileService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import me.lucko.helper.Services;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineCache {

    public static @NonNull MineCache create(@NonNull RedisService redisService) {
        return new MineCache(redisService);
    }

    private final RedisCache<UUID, Mine> cache;
    private final Map<UUID, Mine> localCache = new ConcurrentHashMap<>();

    private MineCache(RedisService redisService) {
        if (Services.get(MineCache.class).isPresent()) {
            throw new IllegalStateException("MineCache is already initialized");
        }

        final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

        this.cache = RedisCache.newCache("mc.prison.mining:mines", redisService, Mine.class, gson);
        Services.provide(MineCache.class, this);
    }

    public void insert(@NonNull Mine mine) {
        this.cache.insert(mine.getMineId(), mine);
        this.localCache.put(mine.getMineId(), mine);
    }

    public @Nullable Mine find(@NonNull UUID mineId) {
        final Mine localMine = this.localCache.get(mineId);
        if (localMine != null) {
            return localMine;
        }

        final Mine mine = this.cache.get(mineId);
        if (mine == null) {
            return null;
        }

        this.localCache.put(mineId, mine);
        return mine;
    }

    public @NotNull Mine findOrCreate(@NonNull PrisonUserProfile profile) {
        if (profile.getMineId() != null) {
            final Mine existingMine = this.find(profile.getMineId());
            if (existingMine != null) {
                return existingMine;
            }
        }

        final Mine mine = new Mine(profile);
        mine.updateTheme(MineTheme.defaultTheme());
        this.insert(mine);

        profile.setMineId(mine.getMineId());

        PrisonUserProfileService.get().upsert(profile);
        return mine;
    }

    public void invalidateLocal(@NonNull Mine mine) {
        this.localCache.remove(mine.getMineId());
    }
}

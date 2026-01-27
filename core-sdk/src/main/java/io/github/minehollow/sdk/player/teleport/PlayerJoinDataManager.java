package io.github.minehollow.sdk.player.teleport;

import io.github.minehollow.sdk.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerJoinDataManager {

    private static final PlayerJoinDataManager INSTANCE = new PlayerJoinDataManager();

    public static PlayerJoinDataManager getInstance() {
        return INSTANCE;
    }


    private final RedisCache<PlayerJoinData> redisCache = new RedisCache<>(PlayerJoinData.class);

    PlayerJoinDataManager() {
    }

    public PlayerJoinData getPlayerJoinData(@NotNull UUID playerId) {
        return redisCache.get("playerJoinData:" + playerId.toString());
    }

    public void setJoinData(@NotNull PlayerJoinData joinData) {
        redisCache.set("playerJoinData:" + joinData.uniqueId().toString(), joinData, 10);
    }

    public void removeJoinData(@NotNull UUID playerId) {
        redisCache.hdel("playerJoinData:", playerId.toString());
    }
}

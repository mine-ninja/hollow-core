package net.warcane.lugin.core.player.fetcher;

import net.warcane.lugin.core.util.data.RedisCache;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Classe responsavel por buscar o UUID de um jogador.
 *
 * @deprecated Use {@link net.warcane.lugin.core.player.account.PlayerAccountService#loadFromRedis(UUID)} em vez disso.
 */
@Deprecated(forRemoval = true)
public class PlayerUuidFetcher {
    private static final class PlayerUuidFetcherHolder {
        private static final PlayerUuidFetcher INSTANCE = new PlayerUuidFetcher();
    }
    
    public static PlayerUuidFetcher getInstance() {
        return PlayerUuidFetcherHolder.INSTANCE;
    }
    
    private final RedisCache<UUID> redisCache;
    
    private PlayerUuidFetcher() {
        this.redisCache = new RedisCache<>(UUID.class);
    }
    
    @Nullable
    public UUID fetchPlayerUuid(String playerName) {
        return redisCache.get("pid:" + playerName.toLowerCase());
    }
    
    public void cachePlayerUuid(String playerName, UUID playerUuid) {
        redisCache.set("pid:" + playerName.toLowerCase(), playerUuid, 86400); // Armazena por 24 horas
    }
}

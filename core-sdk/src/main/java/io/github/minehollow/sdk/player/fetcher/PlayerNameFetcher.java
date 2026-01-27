package io.github.minehollow.sdk.player.fetcher;

import io.github.minehollow.sdk.player.account.PlayerAccountService;
import io.github.minehollow.sdk.util.data.RedisCache;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Classe responsavel por buscar o nome de um jogador.
 *
 * @deprecated Use {@link PlayerAccountService#loadFromRedisByName(String)} em vez disso.
 */
@Deprecated(forRemoval = true)
public class PlayerNameFetcher {
    private static final long ONE_WEEK_IN_SECONDS = 604_800L; // 7 dias em segundos
    private static final String IDX = "player_name";
    
    private static final class PlayerNameFetcherHolder {
        private static final PlayerNameFetcher INSTANCE = new PlayerNameFetcher();
    }
    
    public static PlayerNameFetcher getInstance() {
        return PlayerNameFetcherHolder.INSTANCE;
    }
    
    private final RedisCache<String> playerNameCache = new RedisCache<>(String.class);
    
    public String getPlayerName(@NotNull UUID playerId) {
        return playerNameCache.get(IDX + ":" + playerId);
    }
    
    public void setPlayerName(@NotNull UUID playerId, @NotNull String playerName) {
        playerNameCache.set(IDX + ":" + playerId, playerName, ONE_WEEK_IN_SECONDS);
    }
}

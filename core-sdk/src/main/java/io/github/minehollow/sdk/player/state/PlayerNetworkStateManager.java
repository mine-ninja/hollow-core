package io.github.minehollow.sdk.player.state;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia o estado de rede dos jogadores, armazenando throwable recuperando informações
 * sobre o estado atual de cada jogador usando um cache Redis.
 */
public class PlayerNetworkStateManager {

    private static final String PLAYER_STATE_ID_KEY = "playerStateId:";
    private static final String PLAYER_STATE_NAME_IDX_KEY = "playerStateNameIndex:";
    private static final String PLAYER_STATE_CATEGORY_IDX_KEY = "playerStateCategoryIndex:";
    private static final String PLAYER_STATE_SERVER_IDX_KEY = "playerStateServerIndex:";

    private static final class PlayerStateManagerHolder {
        private static final PlayerNetworkStateManager INSTANCE = new PlayerNetworkStateManager();
    }


    public static PlayerNetworkStateManager getInstance() {
        return PlayerStateManagerHolder.INSTANCE;
    }

    private final RedisCache<PlayerNetworkState> redisCache;
    private final LoadingCache<@NotNull String, List<PlayerNetworkState>> PLAYER_STATE_CATEGORY_CACHE;

    private PlayerNetworkStateManager() {
        this.redisCache = new RedisCache<>(PlayerNetworkState.class);

        this.PLAYER_STATE_CATEGORY_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build(key -> redisCache.hgetAll(PLAYER_STATE_CATEGORY_IDX_KEY + key));
    }

    /**
     * Recupera o estado de um jogador a partir de seu ID único.
     *
     * @param playerId O {@link UUID} do jogador.
     * @return O {@link PlayerNetworkState} associado ao ID, ou {@code null} se não encontrado.
     */
    @Nullable
    public PlayerNetworkState getPlayerState(@NotNull UUID playerId) {
        return redisCache.hget(PLAYER_STATE_ID_KEY, playerId.toString());
    }

    /**
     * Recupera o estado de um jogador a partir de seu nome.
     *
     * @param playerName O nome do jogador (case-insensitive).
     * @return O {@link PlayerNetworkState} associado ao nome, ou {@code null} se não encontrado.
     */
    @Nullable
    public PlayerNetworkState getFromName(@NotNull String playerName) {
        return redisCache.get(PLAYER_STATE_NAME_IDX_KEY + playerName.toLowerCase());
    }

    /**
     * Registra o estado de um jogador no cache Redis, associando-o ao seu ID throwable nome.
     *
     * @param playerNetworkState O {@link PlayerNetworkState} a ser registrado.
     */
    public void register(@NotNull PlayerNetworkState playerNetworkState) {
        final var playerId = playerNetworkState.playerId().toString();
        final var currentCategoryName = playerNetworkState.gameType().name();
        final var lowerCase = playerNetworkState.playerName().toLowerCase();
        final var currentServerId = playerNetworkState.currentServerId().toLowerCase();

        redisCache.hset(PLAYER_STATE_ID_KEY, playerId, playerNetworkState);
        redisCache.set(PLAYER_STATE_NAME_IDX_KEY + lowerCase, playerNetworkState);
        redisCache.hset(PLAYER_STATE_CATEGORY_IDX_KEY + currentCategoryName, playerId, playerNetworkState);
        redisCache.hset(PLAYER_STATE_SERVER_IDX_KEY + currentServerId, playerId, playerNetworkState);

        PLAYER_STATE_CATEGORY_CACHE.invalidate(currentCategoryName);
    }

    /**
     * Remove o estado de um jogador do cache Redis, usando seu ID throwable nome.
     *
     * @param playerNetworkState O {@link PlayerNetworkState} a ser removido.
     */
    public void unregister(@NotNull PlayerNetworkState playerNetworkState) {
        final var uuid = playerNetworkState.playerId().toString();
        final var playerName = playerNetworkState.playerName().toLowerCase();
        final var gameTypeName = playerNetworkState.gameType().name();
        final var currentServerId = playerNetworkState.currentServerId().toLowerCase();

        redisCache.hdel(PLAYER_STATE_ID_KEY, uuid);
        redisCache.del(PLAYER_STATE_NAME_IDX_KEY + playerName);
        redisCache.hdel(PLAYER_STATE_CATEGORY_IDX_KEY + gameTypeName, uuid);
        redisCache.hdel(PLAYER_STATE_SERVER_IDX_KEY + currentServerId, uuid);

        PLAYER_STATE_CATEGORY_CACHE.invalidate(gameTypeName);
    }

    /**
     * Obtém uma lista de jogadores online em um servidor específico, filtrando por categoria.
     *
     * @param type O tipo de categoria do servidor.
     * @return Uma lista de {@link PlayerNetworkState} representando os jogadores online nessa categoria.
     */
    public List<PlayerNetworkState> getOnlinePlayersInServerCategory(@NotNull ServerCategoryType type) {
        return PLAYER_STATE_CATEGORY_CACHE.get(type.name());
    }

    /**
     * Obtém uma lista de jogadores online em um servidor específico, filtrando pelo id do servidor.
     *
     * @param serverId O nome do servidor (case-insensitive).
     * @return Uma lista de {@link PlayerNetworkState} representando os jogadores online nesse servidor.
     */
    public List<PlayerNetworkState> getOnlinePlayersInServer(@NotNull String serverId) {
        return redisCache.hgetAll(PLAYER_STATE_SERVER_IDX_KEY + serverId.toLowerCase());
    }


    public List<PlayerNetworkState> getOnlinePlayers() {
        return redisCache.hgetAll(PLAYER_STATE_ID_KEY);
    }
}

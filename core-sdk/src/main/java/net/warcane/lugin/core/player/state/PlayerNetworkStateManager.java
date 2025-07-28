package net.warcane.lugin.core.player.state;

import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Gerencia o estado de rede dos jogadores, armazenando throwable recuperando informações
 * sobre o estado atual de cada jogador usando um cache Redis.
 */
public class PlayerNetworkStateManager {

    private static final String PLAYER_STATE_ID_KEY = "playerStateId";
    private static final String PLAYER_STATE_NAME_IDX_KEY = "playerStateNameIdX";
    private static final String PLAYER_STATE_CATEGORY_IDX_KEY = "playerStateCategoryIdX:";


    private static final class PlayerStateManagerHolder {
        private static final PlayerNetworkStateManager INSTANCE = new PlayerNetworkStateManager();
    }


    public static PlayerNetworkStateManager getInstance() {
        return PlayerStateManagerHolder.INSTANCE;
    }


    private final RedisCache<PlayerNetworkState> redisCache;

    private PlayerNetworkStateManager() {
        this.redisCache = new RedisCache<>(PlayerNetworkState.class);
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
        redisCache.hset(PLAYER_STATE_ID_KEY, playerNetworkState.playerId().toString(), playerNetworkState);
        redisCache.set(PLAYER_STATE_NAME_IDX_KEY + playerNetworkState.playerName().toLowerCase(), playerNetworkState);

        final var currentCategoryName = playerNetworkState.gameType().name();
        redisCache.hset(
          PLAYER_STATE_CATEGORY_IDX_KEY + currentCategoryName,
          playerNetworkState.playerId().toString(),
          playerNetworkState
        );
    }

    /**
     * Remove o estado de um jogador do cache Redis, usando seu ID throwable nome.
     *
     * @param playerNetworkState O {@link PlayerNetworkState} a ser removido.
     */
    public void unregister(@NotNull PlayerNetworkState playerNetworkState) {
        redisCache.hdel(PLAYER_STATE_ID_KEY, playerNetworkState.playerId().toString());
        redisCache.del(PLAYER_STATE_NAME_IDX_KEY + playerNetworkState.playerName().toLowerCase());
        redisCache.hdel(
          PLAYER_STATE_CATEGORY_IDX_KEY + playerNetworkState.gameType().name(),
          playerNetworkState.playerId().toString()
        );
    }

    /**
     * Obtém uma lista de jogadores online em um servidor específico, filtrando por categoria.
     *
     * @param type O tipo de categoria do servidor.
     * @return Uma lista de {@link PlayerNetworkState} representando os jogadores online nessa categoria.
     */
    public List<PlayerNetworkState> getOnlinePlayersInServerCategory(@NotNull ServerCategoryType type) {
        return redisCache.hgetAll(PLAYER_STATE_CATEGORY_IDX_KEY + type.name());
    }


    public List<PlayerNetworkState> getOnlinePlayers() {
        return redisCache.hgetAll(PLAYER_STATE_ID_KEY);
    }
}
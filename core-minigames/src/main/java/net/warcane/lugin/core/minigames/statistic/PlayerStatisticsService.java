package net.warcane.lugin.core.minigames.statistic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Interface para gerenciamento assíncrono de estatísticas de jogadores.
 */
public interface PlayerStatisticsService {

    /**
     * Cria uma nova instância do serviço de estatísticas de jogadores.
     *
     * @param executorService ExecutorService para operações assíncronas (não nulo).
     * @return Nova instância do PlayerStatisticsService.
     */
    static @NotNull PlayerStatisticsService of(@NotNull ExecutorService executorService) {
        return new PlayerStatisticsServiceImpl(executorService);
    }

    /**
     * Obtém os dados do jogador do cache local, se disponível.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return Estatísticas do jogador ou null se não estiver no cache.
     */
    @Nullable PlayerStatistics getCachedAccount(@NotNull UUID playerId);

    /**
     * Busca a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a conta do jogador ou null se não encontrada.
     */
    CompletableFuture<@Nullable PlayerStatistics> getPlayerAccount(@NotNull UUID playerId);

    /**
     * Carrega a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a conta carregada.
     */
    CompletableFuture<@NotNull PlayerStatistics> loadPlayerAccount(@NotNull UUID playerId);

    /**
     * Descarrega a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a conta descarregada.
     */
    CompletableFuture<@NotNull PlayerStatistics> unloadPlayerAccount(@NotNull UUID playerId);
}

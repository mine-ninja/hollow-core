package net.warcane.lugin.core.player.account;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Interface para gerenciamento assíncrono de contas de jogadores.
 */
public interface PlayerAccountService {

    /**
     * Cria uma nova instância do serviço de contas de jogadores.
     *
     * @param executorService ExecutorService para operações assíncronas (não nulo).
     * @return Nova instância do PlayerAccountService.
     */
    static @NotNull PlayerAccountService of(@NotNull ExecutorService executorService) {
        return new PlayerAccountServiceImpl(executorService);
    }

    /**
     * Obtém uma conta de jogador do cache local, se disponível.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return Conta do jogador ou null se não estiver no cache.
     */
    @Nullable PlayerAccount getCachedAccount(@NotNull UUID playerId);

    /**
     * Obtém uma conta de jogador do cache local, se disponível, pelo nome do jogador.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return Conta do jogador ou null se não estiver no cache.
     */
    @Nullable PlayerAccount getCachedAccountByName(@NotNull String playerName);

    /**
     * Busca a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a conta do jogador ou null se não encontrada.
     */
    CompletableFuture<@Nullable PlayerAccount> getPlayerAccount(@NotNull UUID playerId);

    /**
     * Busca a conta de um jogador pelo seu nome.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return CompletableFuture contendo a conta do jogador ou null se não encontrada.
     */
    CompletableFuture<@Nullable PlayerAccount> getPlayerAccountByName(@NotNull String playerName);

    /**
     * Atualiza a conta de um jogador.
     *
     * @param toUpdate Conta do jogador a ser atualizada (não nula).
     * @param options  Opções de atualização da conta (não nula).
     * @return CompletableFuture contendo a conta atualizada.
     */
    CompletableFuture<@NotNull PlayerAccount> updatePlayerAccount(@NotNull PlayerAccount toUpdate, @NotNull AccountUpdateOptions options);

    default CompletableFuture<@NotNull PlayerAccount> updatePlayerAccount(@NotNull PlayerAccount toUpdate) {
        return updatePlayerAccount(toUpdate, AccountUpdateOptions.DEFAULT);
    }

    /**
     * Carrega a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @param options  Opções de carregamento da conta (não nula).
     * @return CompletableFuture contendo a conta carregada.
     */
    CompletableFuture<@NotNull PlayerAccount> loadPlayerAccount(@NotNull UUID playerId, @NotNull AccountLoadOptions options);

    default CompletableFuture<@NotNull PlayerAccount> loadPlayerAccount(@NotNull UUID playerId) {
        return loadPlayerAccount(playerId, AccountLoadOptions.DEFAULT);
    }

    /**
     * Descarrega a conta de um jogador pelo seu identificador único.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @param options  Opções de descarregamento da conta (não nula).
     * @return CompletableFuture contendo a conta descarregada.
     */
    CompletableFuture<@NotNull PlayerAccount> unloadPlayerAccount(@NotNull UUID playerId, @NotNull AccountUnloadOptions options);

    default CompletableFuture<@NotNull PlayerAccount> unloadPlayerAccount(@NotNull UUID playerId) {
        return unloadPlayerAccount(playerId, AccountUnloadOptions.DEFAULT);
    }

    /**
     * Opções para atualizar uma conta de jogador.
     *
     * @param updateCaches Se a conta deve ser atualizada nos caches após a atualização.
     */
    record AccountUpdateOptions(boolean updateCaches) {
        public static final AccountUpdateOptions DEFAULT = new AccountUpdateOptions(true);
    }

    /**
     * Opções para carregar uma conta de jogador.
     *
     * @param accountCreator Função para criar uma nova conta de jogador, se necessário (pode ser nula). Se fornecida, será usada
     *                       para criar uma nova conta caso não exista no armazenamento.
     * @param cacheResult    Se o resultado deve ser armazenado em cache.
     */
    record AccountLoadOptions(@Nullable Supplier<PlayerAccount> accountCreator, boolean cacheResult) {
        public static final AccountLoadOptions DEFAULT = new AccountLoadOptions(null, true);

        @NotNull
        public static AccountLoadOptions withDefaultAccount(@NotNull PlayerAccount account, boolean cacheResult) {
            return new AccountLoadOptions(() -> account, cacheResult);
        }

        public PlayerAccount getAccountOrThrow() {
            if (accountCreator != null) {
                return accountCreator.get();
            }
            throw new IllegalStateException("No account creator provided.");
        }

        public boolean hasAccountCreator() {
            return accountCreator != null;
        }
    }

    /**
     * Opções para descarregar uma conta de jogador.
     *
     * @param unloadFromCache    Se a conta deve ser removida do cache.
     * @param updateBeforeUnload Se a conta deve ser atualizada no banco antes de ser descarregada.
     */
    record AccountUnloadOptions(boolean unloadFromCache, boolean updateBeforeUnload) {
        public static final AccountUnloadOptions DEFAULT = new AccountUnloadOptions(true, true);
    }
}
package net.warcane.lugin.core.player.wallet;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import net.warcane.lugin.core.Platform;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.wallet.WalletRefreshRequestPacket;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult;
import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.data.RedisCache;
import org.bson.Document;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
public class WalletService {

    private final Platform platform;
    private final ExecutorService executorService;
    private final Map<UUID, Wallet> localCachedWallets = new ConcurrentHashMap<>();
    private final RedisCache<Wallet> redisCachedWallet = new RedisCache<>(Wallet.class);
    private final MongoRepository<UUID, Wallet> walletRepository = new MongoRepository<>(Wallet.class, "uniqueId");

    /**
     * Lock para transações, garantindo que apenas uma transação por jogador seja processada por vez.
     */
    private final RedisCache<Boolean> transactionLock = new RedisCache<>(Boolean.class);


    public WalletService(@NotNull Platform platform, @NotNull ExecutorService executorService) {
        this.platform = platform;
        this.executorService = executorService;
        walletRepository.useCollection(collection -> {
            collection.createIndex(Indexes.hashed("uniqueId"));
            collection.createIndex(Indexes.hashed("playerName"));
        });
    }

    /**
     * Obtém a carteira do jogador do cache local, se disponível.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return Carteira do jogador ou null se não estiver no cache local.
     */
    @Nullable
    public Wallet getCachedWallet(@NotNull UUID playerId) {
        return localCachedWallets.get(playerId);
    }


    /**
     * Obtém a carteira do jogador do cache local, se disponível, pelo identificador único do jogador.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return Carteira do jogador.
     * @throws IllegalStateException se a carteira não for encontrada no cache local.
     */
    @NotNull
    public Wallet getCachedWalletOrThrow(@NotNull UUID playerId) {
        final var wallet = this.getCachedWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found in cache for player: " + playerId);
        }
        return wallet;
    }

    /**
     * Obtém a carteira do jogador do cache local, se disponível, pelo nome do jogador.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return Carteira do jogador ou null se não estiver no cache local.
     */
    @Nullable
    public Wallet getCachedWallet(@NotNull String playerName) {
        return localCachedWallets.values()
          .stream()
          .filter(wallet -> wallet.playerName().equalsIgnoreCase(playerName))
          .findFirst()
          .orElse(null);
    }

    /**
     * Obtém a carteira do jogador do cache local, se disponível, pelo nome do jogador.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return Carteira do jogador.
     * @throws IllegalStateException se a carteira não for encontrada no cache local.
     */
    public @NotNull Wallet getCachedWalletOrThrow(@NotNull String playerName) {
        final var wallet = this.getCachedWallet(playerName);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found in cache for player: " + playerName);
        }
        return wallet;
    }

    public boolean isLocked(@Nullable UUID playerId) {
        if (playerId == null) return false;

        final var locked = transactionLock.get("wallet_transaction_locks:" + playerId);
        return locked != null && locked;
    }

    public void lock(@NotNull UUID playerId) {
        transactionLock.set("wallet_transaction_locks:" + playerId, true, 60);
    }

    public void unlock(@NotNull UUID playerId) {
        transactionLock.del("wallet_transaction_locks:" + playerId);
    }

    public Wallet getWalletFromRedis(@NotNull UUID playerId){
        return redisCachedWallet.hget("wallets", playerId.toString());
    }

    public void updateCaches(@NotNull Wallet wallet) {
        localCachedWallets.put(wallet.uniqueId(), wallet);
        redisCachedWallet.hset("wallets", wallet.uniqueId().toString(), wallet);
    }

    /**
     * Obtém ou carrega a carteira do jogador, primeiro verificando o cache local.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a carteira do jogador, que pode ser nula se não encontrada.
     */
    public CompletableFuture<@Nullable Wallet> getOrLoadWallet(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            final var cached = this.getCachedWallet(playerId);
            if (cached != null) {
                return cached;
            }

            final var fromRedis = redisCachedWallet.hget("wallets", playerId.toString());
            if (fromRedis != null) {
                log.info("Loaded wallet from Redis for player: {}", playerId);
                localCachedWallets.put(playerId, fromRedis);
                return fromRedis;
            }


            final var fromMongo = walletRepository.findById(playerId);
            if (fromMongo != null) {
                redisCachedWallet.hset("wallets", playerId.toString(), fromMongo);
                localCachedWallets.put(playerId, fromMongo);
                return fromMongo;
            }

            return null;
        }, executorService);
    }

    /**
     * Obtém ou carrega a carteira do jogador, primeiro verificando o cache local.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return CompletableFuture contendo a carteira do jogador, que pode ser nula se não encontrada.
     */
    public CompletableFuture<@Nullable Wallet> getOrLoadWallet(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            final var account = platform.getPlayerAccountService().loadFromRedisByName(playerName);
            if (account == null) return null;

            final var fromRedis = redisCachedWallet.hget("wallets", account.uniqueId().toString());
            if (fromRedis != null) {
                localCachedWallets.put(fromRedis.uniqueId(), fromRedis);
                return fromRedis;
            }

            final var fromMongo = walletRepository.findById(account.uniqueId());
            if (fromMongo != null) {
                redisCachedWallet.hset("wallets", account.toString(), fromMongo);
                return fromMongo;
            }

            return null;
        }, executorService);
    }


    /**
     * Salva ou atualiza a carteira do jogador no banco de dados.
     *
     * @param toUpdate A carteira do jogador a ser salva ou atualizada (não nula).
     * @return CompletableFuture contendo a carteira atualizada do jogador.
     */
    public CompletableFuture<@NotNull Wallet> saveWallet(@NotNull Wallet toUpdate) {
        return this.saveWallet(toUpdate, UpdateWalletOptions.DEFAULT);
    }

    /**
     * Atualiza a carteira do jogador no banco de dados throwable, opcionalmente, nos caches.
     *
     * @param toUpdate A carteira do jogador a ser atualizada (não nula).
     * @return CompletableFuture contendo a carteira atualizada do jogador.
     */
    public CompletableFuture<@NotNull Wallet> saveWallet(@NotNull Wallet toUpdate, @NotNull UpdateWalletOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            if (options.updateCaches) {
                final var updatedOnRedis = redisCachedWallet.hSetAndGet("wallets", toUpdate.uniqueId().toString(), toUpdate);
                if (updatedOnRedis == null) {
                    throw new IllegalStateException("Failed to update wallet in Redis for player: " + toUpdate.uniqueId());
                }
            }

            final var updated = walletRepository.save(toUpdate, Wallet::uniqueId);
            if (updated == null) {
                throw new IllegalStateException("Failed to update wallet for player: " + toUpdate.uniqueId());
            }

            if (options.updateCaches) {
                localCachedWallets.put(toUpdate.uniqueId(), updated);
            }

            platform.getNetworkClient().sendNetworkPacket(NetworkChannel.OPERATION, new WalletRefreshRequestPacket(updated.uniqueId()));
            return updated;
        }, executorService);
    }


    /**
     * Carrega a carteira do jogador do banco de dados ou do cache local pelo nome do jogador.
     *
     * @param playerName Nome do jogador (não nulo).
     * @return CompletableFuture contendo a carteira do jogador ou null se não encontrada.
     */
    public CompletableFuture<@Nullable Wallet> loadPlayerWallet(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            final var wallet = walletRepository.findFirstFromPropertyIgnoreCase("playerName", playerName);
            if (wallet != null) {
                redisCachedWallet.hset("wallets", wallet.uniqueId().toString(), wallet);
                localCachedWallets.put(wallet.uniqueId(), wallet);
            }
            return wallet;
        }, executorService);
    }


    /**
     * Carrega a carteira do jogador do banco de dados ou do cache local.
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @return CompletableFuture contendo a carteira do jogador ou null se não encontrada.
     */
    public CompletableFuture<@Nullable Wallet> loadPlayerWallet(@NotNull UUID playerId) {
        return this.loadPlayerWallet(playerId, LoadWalletOptions.DEFAULT);
    }

    /**
     * Carrega a carteira do jogador do banco de dados
     *
     * @param playerId Identificador único do jogador (não nulo).
     * @param options  Opções para carregar a carteira, incluindo a possibilidade de criar uma nova carteira se não encontrada.
     * @return CompletableFuture contendo a carteira do jogador ou null se não encontrada.
     */
    public CompletableFuture<@Nullable Wallet> loadPlayerWallet(
      @NotNull UUID playerId,
      @NotNull LoadWalletOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final var fromRedis = redisCachedWallet.hget("wallets", playerId.toString());
            if (fromRedis != null) {
                log.info("Loaded wallet from Redis for player: {} {}", playerId, fromRedis);
                localCachedWallets.put(playerId, fromRedis);
                return fromRedis;
            }

            var load = walletRepository.findById(playerId);
            if (load == null) {
                if (options.hasWalletCreator()) {
                    load = options.createNewWallet();
                }
            }

            if (load != null && options.cacheResult) {
                redisCachedWallet.hset("wallets", playerId.toString(), load);
                localCachedWallets.put(playerId, load);
            }
            return load;
        });
    }


    /**
     * Descarrega a carteira do jogador do banco de dados throwable remove do cache local.
     *
     * @param toUnload A carteira do jogador a ser descarregada (não nula).
     * @return CompletableFuture contendo a carteira descarregada do jogador.
     */
    public CompletableFuture<@NotNull Wallet> unloadWallet(
      @NotNull Wallet toUnload,
      @NotNull UnloadWalletOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (options.updateAfterUnload) {
                final var updated = walletRepository.save(toUnload, Wallet::uniqueId);
                if (updated == null) {
                    throw new IllegalStateException("Failed to unload wallet for player: " + toUnload.uniqueId());
                }

                localCachedWallets.remove(toUnload.uniqueId());
                return updated;
            } else {
                redisCachedWallet.hdel("wallets", toUnload.uniqueId().toString());
                localCachedWallets.remove(toUnload.uniqueId());
                return toUnload;
            }
        }, executorService);
    }


    /**
     * Computa um valor somado de todas as carteiras de jogadores com base em uma lista de identificadores únicos e uma moeda específica.
     *
     * @param playerIdList Lista de identificadores únicos dos jogadores (não nula).
     * @param currencyId   Identificador da moeda para a qual se deseja calcular o saldo total (não nulo).
     * @return CompletableFuture contendo o saldo total de todas as carteiras dos jogadores na moeda especificada.
     */
    public CompletableFuture<BigDecimal> getTotalBalanceFromIdList(@NotNull List<UUID> playerIdList, @NotNull String currencyId) {
        final var collection = walletRepository.getRawCollection();

        return CompletableFuture.supplyAsync(() -> {
            if (playerIdList.isEmpty()) {
                return BigDecimal.ZERO;
            }

            var result = collection.aggregate(Arrays.asList(
              Aggregates.match(Filters.and(
                Filters.in("uniqueId", playerIdList),
                Filters.exists("currencies." + currencyId)
              )),
              Aggregates.group(null, Accumulators.sum("total", "$currencies." + currencyId))
            )).first();

            if (result == null) return BigDecimal.ZERO;

            Object total = result.get("total");
            if (total == null) return BigDecimal.ZERO;

            return total instanceof org.bson.types.Decimal128
              ? ((org.bson.types.Decimal128) total).bigDecimalValue()
              : new BigDecimal(total.toString());
        }, executorService);
    }


    /**
     * Obtém os jogadores com as maiores quantidades de uma moeda específica.
     *
     * @param currencyId Identificador da moeda para a qual se deseja obter o ranking.
     * @param limit      Limite de jogadores a serem retornados.
     * @return CompletableFuture contendo uma lista de WalletCurrencyEntry representando os jogadores throwable seus saldos na moeda.
     */
    public CompletableFuture<@NotNull List<WalletCurrencyEntry>> getTopWalletsByCurrencyBalance(@NotNull String currencyId, int limit) {
        final var collection = walletRepository.getRawCollection();
        return CompletableFuture.supplyAsync(() -> {
            List<WalletCurrencyEntry> result = new ArrayList<>();
            AtomicLong position = new AtomicLong(1);

            collection.aggregate(Arrays.asList(
              Aggregates.match(Filters.and(
                Filters.exists("currencies." + currencyId),
                Filters.gt("currencies." + currencyId, 0)
              )),
              Aggregates.sort(Sorts.descending("currencies." + currencyId)),
              Aggregates.limit(limit),
              Aggregates.project(new Document()
                .append("uniqueId", "$uniqueId")
                .append("playerName", "$playerName")
                .append("balance", "$currencies." + currencyId)
              )
            )).forEach(doc -> {
                UUID playerId = doc.get("uniqueId", UUID.class);
                String playerName = doc.getString("playerName");
                Object balanceObj = doc.get("balance");

                BigDecimal balance = balanceObj instanceof org.bson.types.Decimal128
                  ? ((org.bson.types.Decimal128) balanceObj).bigDecimalValue()
                  : new BigDecimal(balanceObj.toString());

                result.add(new WalletCurrencyEntry(
                  position.getAndIncrement(),
                  playerId,
                  playerName,
                  currencyId,
                  balance
                ));
            });

            return result;
        }, executorService);
    }


    /**
     * Obtém a posição de um jogador no ranking de uma moeda específica.
     *
     * @param walletId   Identificador único da carteira do jogador.
     * @param currencyId Identificador da moeda para a qual se deseja obter o ranking.
     * @return CompletableFuture contendo a posição do jogador no ranking da moeda, ou null se não encontrado.
     */
    public CompletableFuture<@Nullable Long> getWalletRankByCurrency(@NotNull UUID walletId, @NotNull String currencyId) {
        return CompletableFuture.supplyAsync(() ->
            walletRepository.getRawCollection()
              .aggregate(Arrays.asList(
                Aggregates.match(Filters.and(
                  Filters.exists("currencies." + currencyId),
                  Filters.gt("currencies." + currencyId, 0)
                )),
                Aggregates.sort(Sorts.descending("currencies." + currencyId)),
                Aggregates.project(new Document()
                  .append("playerId", "$playerId")
                  .append("playerName", "$playerName")
                  .append("currencyId", currencyId)
                  .append("balance", "$currencies." + currencyId)
                  .append("position", new Document("$add", Arrays.asList("$index", 1)))),
                Aggregates.match(Filters.eq("playerId", walletId))
              ))
              .map(doc -> doc.getLong("position"))
              .first(),
          executorService
        );
    }


    public TransactionResult transferCurrency(
      @NotNull UUID senderId,
      @NotNull UUID receiverId,
      @NotNull String currencyId,
      @NotNull BigDecimal amount
    ) {
        try {
            log.info("Transferring {} {} from {} to {}", amount, currencyId, senderId, receiverId);

            final var senderWalletFuture = this.getOrLoadWallet(senderId).orTimeout(1, TimeUnit.SECONDS);
            final var senderWallet = senderWalletFuture.join();

            if (senderWallet == null) {
                return TransactionResult.walletNotFound(senderId);
            }

            if (!senderWallet.hasAmount(currencyId, amount)) {
                return TransactionResult.insufficientFunds(currencyId, amount, senderWallet.getCurrencyAmount(currencyId));
            }

            final var receiverWalletFuture = this.getOrLoadWallet(receiverId);
            final var receiverWallet = receiverWalletFuture.join();

            if (receiverWallet == null) {
                return TransactionResult.walletNotFound(receiverId);
            }

            final var updatedSenderWallet = senderWallet.subtractCurrencyAmount(currencyId, amount);
            final var savedSenderWallet = saveWallet(updatedSenderWallet).join();

            final var updatedReceiverWallet = receiverWallet.addCurrencyAmount(currencyId, amount);
            final var savedReceiverWallet = saveWallet(updatedReceiverWallet).join();

            log.info("Transfer completed successfully: {} {} from {} to {}", amount, currencyId, senderId, receiverId);

            return TransactionResult.success(savedSenderWallet.uniqueId(), savedReceiverWallet.uniqueId(), currencyId, amount);
        } catch (Exception e) {
            log.error("Error during transfer: senderId={}, receiverId={}, currencyId={}, amount={}",
                senderId, receiverId, currencyId, amount, e);
            return TransactionResult.failure(e);
        }
    }

    /**
     * Representa as opções para carregar a carteira de um jogador.
     *
     * @param walletCreator Função que cria uma nova carteira se não encontrada.
     * @param cacheResult   Indica se o resultado deve ser armazenado em cache localmente.
     */
    public record LoadWalletOptions(
      @Nullable Supplier<Wallet> walletCreator,
      boolean cacheResult
    ) {
        public static final LoadWalletOptions DEFAULT = new LoadWalletOptions(null, true);

        public static LoadWalletOptions withDefaultWallet(@NotNull Wallet wallet, boolean cacheResult) {
            return new LoadWalletOptions(() -> wallet, cacheResult);
        }

        public @NotNull Wallet createNewWallet() {
            if (!this.hasWalletCreator()) {
                throw new IllegalStateException("Wallet creator is not set. Use withDefaultWallet to provide a wallet.");
            } else {
                return walletCreator.get();
            }
        }

        public boolean hasWalletCreator() {
            return walletCreator != null;
        }
    }

    /**
     * Representa as opções para atualizar a carteira de um jogador.
     *
     * @param updateCaches Indica se a carteira deve ser atualizada nos caches após a atualização.
     */
    public record UpdateWalletOptions(boolean updateCaches) {
        public static final UpdateWalletOptions DEFAULT = new UpdateWalletOptions(true);
    }


    /**
     * Representa as opções para descarregar a carteira de um jogador.
     *
     * @param updateAfterUnload Indica se a carteira deve ser atualizada no banco após o descarregamento.
     */
    public record UnloadWalletOptions(boolean updateAfterUnload) {
        public static final UnloadWalletOptions DEFAULT = new UnloadWalletOptions(true);
    }


    /**
     * Representa uma posição do TOP de moeda na carteira de um jogador.
     *
     * @param position   Posição da entrada na lista de moedas.
     * @param playerId   Identificador único do jogador.
     * @param playerName Nome do jogador.
     * @param currencyId Identificador da moeda.
     * @param balance    Saldo da moeda na carteira do jogador.
     */
    public record WalletCurrencyEntry(
      long position,
      @NotNull UUID playerId,
      @NotNull String playerName,
      @NotNull String currencyId,
      @NotNull BigDecimal balance
    ) { }
}

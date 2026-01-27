package io.github.minehollow.sdk.player.wallet;

import com.mongodb.client.model.*;
import io.github.minehollow.sdk.Platform;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.wallet.WalletRefreshRequestPacket;
import io.github.minehollow.sdk.player.wallet.log.WalletBalanceLog;
import io.github.minehollow.sdk.player.wallet.log.WalletBalanceLogContainer;
import io.github.minehollow.sdk.player.wallet.transaction.TransactionResult;
import io.github.minehollow.sdk.util.data.MongoRepository;
import io.github.minehollow.sdk.util.data.RedisCache;
import org.bson.Document;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.math.BigDecimal;
import java.util.*;
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
    private final MongoRepository<UUID, WalletBalanceLogContainer> logContainerRepository = new MongoRepository<>(WalletBalanceLogContainer.class, "_id");
    private final RedisCache<Boolean> transactionLock = new RedisCache<>(Boolean.class);

    public WalletService(@NotNull Platform platform, @NotNull ExecutorService executorService) {
        this.platform = platform;
        this.executorService = executorService;
        walletRepository.useCollection(collection -> {
            collection.createIndex(Indexes.hashed("uniqueId"));
            collection.createIndex(Indexes.hashed("playerName"));
        });
    }

    @Nullable
    public Wallet getCachedWallet(@NotNull UUID playerId) {
        return localCachedWallets.get(playerId);
    }

    @NotNull
    public Wallet getCachedWalletOrThrow(@NotNull UUID playerId) {
        final var wallet = this.getCachedWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found in cache for player: " + playerId);
        }
        return wallet;
    }

    @Nullable
    public Wallet getCachedWallet(@NotNull String playerName) {
        return localCachedWallets.values()
          .stream()
          .filter(wallet -> wallet.playerName().equalsIgnoreCase(playerName))
          .findFirst()
          .orElse(null);
    }

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

    public Wallet getWalletFromRedis(@NotNull UUID playerId) {
        return redisCachedWallet.hget("wallets", playerId.toString());
    }

    public void updateCaches(@NotNull Wallet wallet) {
        localCachedWallets.put(wallet.uniqueId(), wallet);
        redisCachedWallet.hset("wallets", wallet.uniqueId().toString(), wallet);
    }

    public void addLogToContainer(@NotNull UUID playerId, @NotNull WalletBalanceLog log) {
        CompletableFuture.runAsync(() -> {
            try {
                var container = logContainerRepository.findById(playerId);
                if (container == null) {
                    container = WalletBalanceLogContainer.createEmpty(playerId);
                }
                container.addLog(log);
                logContainerRepository.save(container, WalletBalanceLogContainer::playerId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executorService);
    }

    public CompletableFuture<WalletBalanceLogContainer> getOrLoadLogContainer(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            var container = logContainerRepository.findById(playerId);
            if (container == null) {
                container = WalletBalanceLogContainer.createEmpty(playerId);
                logContainerRepository.save(container, WalletBalanceLogContainer::playerId);
            }
            return container;
        }, executorService);
    }

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
                redisCachedWallet.hset("wallets", account.uniqueId().toString(), fromMongo);
                return fromMongo;
            }

            return null;
        }, executorService);
    }

    public CompletableFuture<@NotNull Wallet> saveWallet(@NotNull Wallet toUpdate) {
        return this.saveWallet(toUpdate, UpdateWalletOptions.DEFAULT);
    }

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

    public CompletableFuture<@Nullable Wallet> loadPlayerWallet(@NotNull UUID playerId) {
        return this.loadPlayerWallet(playerId, LoadWalletOptions.DEFAULT);
    }

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

    public record UpdateWalletOptions(boolean updateCaches) {
        public static final UpdateWalletOptions DEFAULT = new UpdateWalletOptions(true);
    }

    public record UnloadWalletOptions(boolean updateAfterUnload) {
        public static final UnloadWalletOptions DEFAULT = new UnloadWalletOptions(true);
    }

    public record WalletCurrencyEntry(
      long position,
      @NotNull UUID playerId,
      @NotNull String playerName,
      @NotNull String currencyId,
      @NotNull BigDecimal balance
    ) { }
}
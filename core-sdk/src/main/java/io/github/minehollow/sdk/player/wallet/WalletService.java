package io.github.minehollow.sdk.player.wallet;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.model.*;
import io.github.minehollow.sdk.util.data.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletService {

    private static final FindOneAndUpdateOptions UPDATE_OPTIONS = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);
    private final MongoRepository<UUID, Wallet> walletMongoRepository = new MongoRepository<>(Wallet.class);


    private final Cache<@NotNull UUID, Wallet> localCache = Caffeine.newBuilder()
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .build();

    public @Nullable Wallet getCachedWallet(@NotNull UUID playerId) {
        return localCache.getIfPresent(playerId);
    }

    public void clearCachedWallet(@NotNull UUID playerId) {
        localCache.invalidate(playerId);
    }

    public @Nullable Wallet getOrLoadWallet(@NotNull UUID playerId) {
        var cached = getCachedWallet(playerId);
        return cached != null ? cached : walletMongoRepository.findById(playerId);
    }

    public @Nullable Wallet getOrLoadWallet(@NotNull String playerName) {
        return localCache.asMap().values()
          .stream()
          .filter(w -> w.playerName().equalsIgnoreCase(playerName)).findFirst()
          .orElseGet(() -> walletMongoRepository.findFirstFromProperty("playerName", playerName));
    }

    public @Nullable Wallet loadWallet(@NotNull UUID playerId, @NotNull Wallet defaultWallet, boolean cacheResult) {
        var wallet = walletMongoRepository.findById(playerId);
        if (wallet == null) wallet = defaultWallet;
        if (cacheResult) localCache.put(playerId, wallet);
        return wallet;
    }

    public void updateWallet(@NotNull Wallet wallet) {
        walletMongoRepository.save(wallet.uniqueId(), wallet);
        localCache.put(wallet.uniqueId(), wallet);
    }

    public long getWalletPositionInCurrencyRanking(@NotNull UUID playerId, @NotNull String currencyId) {
        return walletMongoRepository.supplyFromCollection(col -> {
            var wallet = getOrLoadWallet(playerId);
            if (wallet == null) return col.countDocuments();

            BigDecimal amount = wallet.getCurrencyAmount(currencyId);
            String path = "currencies." + currencyId; // Certifique-se que no MongoDB o campo chama 'currencies'

            return col.countDocuments(Filters.or(
              Filters.gt(path, amount),
              Filters.and(Filters.eq(path, amount), Filters.lt("_id", playerId))
            )) + 1;
        });
    }

    public List<WalletRankingEntry> getTopWalletsByCurrency(@NotNull String currencyId, int limit) {
        return walletMongoRepository.supplyFromCollection(col -> {
            String path = "currencies." + currencyId;
            return col.find().sort(Sorts.descending(path)).limit(limit).into(new ArrayList<>()).stream()
              .map(w -> new WalletRankingEntry(w.uniqueId(), w.playerName(), currencyId, w.getCurrencyAmount(currencyId)))
              .toList();
        });
    }

    public @NotNull Wallet incrementCurrencyValue(@NotNull UUID playerId, @NotNull String curId, @NotNull BigDecimal val, boolean cache) {
        return walletMongoRepository.supplyFromCollection(col -> {
            var updated = Objects.requireNonNull(col.findOneAndUpdate(
              Filters.eq("_id", playerId),
              Updates.inc("currencies." + curId, val),
              UPDATE_OPTIONS
            ));
            if (cache) localCache.put(playerId, updated);
            return updated;
        });
    }

    public @NotNull Wallet decrementCurrencyValue(@NotNull UUID playerId, @NotNull String curId, @NotNull BigDecimal val, boolean cache) {
        return incrementCurrencyValue(playerId, curId, val.negate(), cache);
    }

    public Wallet getCachedWalletOrThrow(@NotNull UUID uniqueId) {
        var wallet = getCachedWallet(uniqueId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet for player " + uniqueId + " is not cached.");
        }
        return wallet;
    }

    public record WalletRankingEntry(
      @NotNull UUID playerId,
      @NotNull String playerName,
      @NotNull String currencyId,
      @NotNull BigDecimal amount
    ) {
    }
}
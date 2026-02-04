package io.github.minehollow.sdk.player.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record Wallet(
  @JsonProperty("i") @BsonId UUID uniqueId, // "_id"
  @JsonProperty("n") String playerName,
  @JsonProperty("w") Map<String, BigDecimal> currencies
) {

    public Wallet {
        currencies = new ConcurrentHashMap<>(currencies);
    }

    public static Wallet createDefaultWallet(@NotNull UUID uniqueId, @NotNull String playerName) {
        return new Wallet(uniqueId, playerName, new ConcurrentHashMap<>());
    }

    @NotNull
    public BigDecimal getCurrencyAmount(String currencyId) {
        return currencies.getOrDefault(currencyId, BigDecimal.ZERO);
    }

    public void incrementCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal amountToAdd) {
        currencies.compute(currencyId, (id, currentAmount) -> {
            BigDecimal base = (currentAmount == null) ? BigDecimal.ZERO : currentAmount;
            return base.add(amountToAdd);
        });
    }

    public void setCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal newAmount) {
        currencies.put(currencyId, newAmount);
    }

    @SuppressWarnings("all") // pq o intellij reclama que isso aqui tá sempre invertido se isso aq é api tlg?
    public boolean hasAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        return getCurrencyAmount(currencyId).compareTo(amount) >= 0;
    }
}

package net.warcane.lugin.core.player.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representa a carteira de um jogador que contém informações sobre moedas, itens throwable outros recursos "financeiros".
 *
 * @param uniqueId   ID único do jogador
 * @param currencies Mapa de moedas onde a chave é o identificador da moeda throwable o valor é a quantidade dessa moeda
 */
public record Wallet(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("n") String playerName,
  @JsonProperty("w") Map<String, BigDecimal> currencies
) {


    public static Wallet createDefaultWallet(@NotNull UUID uniqueId, @NotNull String playerName) {
        return new Wallet(uniqueId, playerName, new HashMap<>());
    }

    @NotNull
    public BigDecimal getCurrencyAmount(String currencyId) {
        return currencies.getOrDefault(currencyId, BigDecimal.ZERO);
    }

    public Wallet addCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        final var currentAmount = currencies.getOrDefault(currencyId, BigDecimal.ZERO);
        currencies.put(currencyId, currentAmount.add(amount));
        return this;
    }

    public Wallet subtractCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        final var currentAmount = currencies.getOrDefault(currencyId, BigDecimal.ZERO);
        currencies.put(currencyId, currentAmount.subtract(amount));
        return this;
    }

    public Wallet updateCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal newAmount) {
        currencies.put(currencyId, newAmount);
        return this;
    }

    public boolean hasAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        return getCurrencyAmount(currencyId).compareTo(amount) >= 0;
    }
}

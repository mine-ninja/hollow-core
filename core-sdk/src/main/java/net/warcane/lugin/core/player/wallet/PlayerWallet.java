package net.warcane.lugin.core.player.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Representa a carteira de um jogador que contém informações sobre moedas, itens e outros recursos "financeiros".
 *
 * @param uniqueId   ID único do jogador
 * @param currencies Mapa de moedas onde a chave é o identificador da moeda e o valor é a quantidade dessa moeda
 */
public record PlayerWallet(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("w") Map<String, BigDecimal> currencies
) {

    @NotNull
    public BigDecimal getCurrencyAmount(String currencyId) {
        return currencies.getOrDefault(currencyId, BigDecimal.ZERO);
    }

    public void addCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        final var currentAmount = currencies.getOrDefault(currencyId, BigDecimal.ZERO);
        currencies.put(currencyId, currentAmount.add(amount));
    }

    public void subtractCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal amount) {
        final var currentAmount = currencies.getOrDefault(currencyId, BigDecimal.ZERO);
        currencies.put(currencyId, currentAmount.subtract(amount));
    }

    public void updateCurrencyAmount(@NotNull String currencyId, @NotNull BigDecimal newAmount) {
        currencies.put(currencyId, newAmount);
    }
}

package net.warcane.lugin.core.minecraft.currency;

import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa uma moeda no sistema.
 * Cada moeda possui um identificador único, um nome para exibição, um símbolo throwable um nome plural para exibição.
 *
 * @param id                Identificador único da moeda
 * @param displayName       Nome da moeda para exibição (Ex: "Gold")
 * @param symbol            Símbolo da moeda (Ex: "G" para Gold)
 * @param pluralDisplayName Nome plural da moeda para exibição (Ex: "Golds")
 *
 */
public record Currency(
  @NotNull String id,
  @NotNull String displayName,
  @NotNull String symbol,
  @NotNull String pluralDisplayName,
  @NotNull String commandName,
  @NotNull List<String> commandAliases,
  @NotNull List<ServerCategoryType> allowedCategories,
  boolean allowPlayerPayments
) {

    /**
     * Formata o valor da moeda para exibição.
     * Utiliza o formato padrão de moeda definido pelo CurrencyFormatter.
     *
     * @param amount Quantidade da moeda a ser formatada
     * @return String formatada representando a quantidade da moeda
     */
    public String formatAmount(long amount) {
        final var name = amount == 1 ? displayName : pluralDisplayName;
        return symbol + CurrencyFormatter.formatValue(amount) + " " + name;
    }


    /**
     * Formata o valor da moeda para exibição.
     * Utiliza o formato padrão de moeda definido pelo CurrencyFormatter.
     *
     * @param bigDecimal Quantidade da moeda a ser formatada
     * @return String formatada representando a quantidade da moeda
     */
    public String formatAmount(@NotNull BigDecimal bigDecimal) {
        final var name = bigDecimal.compareTo(BigDecimal.ONE) == 0 ? displayName : pluralDisplayName;
        return symbol + CurrencyFormatter.formatValue(bigDecimal) + " " + name;
    }
}

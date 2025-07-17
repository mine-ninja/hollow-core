package net.warcane.lugin.core.currency;

import org.jetbrains.annotations.NotNull;

/**
 * Representa uma moeda no sistema.
 * Cada moeda possui um identificador único, um nome para exibição, um símbolo e um nome plural para exibição.
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
  @NotNull String pluralDisplayName
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
        return CurrencyFormatter.formatValue(amount);
    }
}

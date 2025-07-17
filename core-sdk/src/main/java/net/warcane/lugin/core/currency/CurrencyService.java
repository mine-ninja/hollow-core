package net.warcane.lugin.core.currency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CurrencyService {

    private final Map<String, Currency> currencies = new HashMap<>();

    public void registerCurrency(@NotNull Currency currency) {
        currencies.put(currency.id(), currency);
    }

    @Nullable
    public Currency getCurrency(@NotNull String id) {
        return currencies.get(id);
    }

    public boolean containsCurrency(@NotNull String id) {
        return currencies.containsKey(id);
    }
}

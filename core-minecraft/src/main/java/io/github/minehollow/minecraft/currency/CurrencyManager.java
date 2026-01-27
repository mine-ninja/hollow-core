package io.github.minehollow.minecraft.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CurrencyManager {


    private final BukkitPlatform platform;
    private final Map<String, Currency> currencies = new HashMap<>();


    public void registerCurrency(@NotNull Currency currency) {
        if (currencies.containsKey(currency.id())) {
            throw new IllegalArgumentException("Currency with id " + currency.id() + " is already registered.");
        }

        currencies.put(currency.id(), currency);
        log.info("Registered currency: {}", currency.id());

        platform.getInternalCommandManager().registerCurrencyCommand(currency);
    }

    @Nullable
    public Currency getCurrency(@NotNull String id) {
        return currencies.get(id);
    }

    public boolean containsCurrency(@NotNull String id) {
        return currencies.containsKey(id);
    }

    @NotNull
    public List<String> getAllCurrencyIds() {
        return List.copyOf(currencies.keySet());
    }

    @NotNull
    public Map<String, Currency> getCurrencies() {
        return currencies;
    }
}

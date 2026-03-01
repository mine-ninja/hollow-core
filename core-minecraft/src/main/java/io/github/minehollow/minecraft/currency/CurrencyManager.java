package io.github.minehollow.minecraft.currency;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.message.MessageConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CurrencyManager {

    private final BukkitPlatform platform;
    @Getter
    private final MessageConfig messageConfig;
    private final Map<String, Currency> currencies = new HashMap<>();

    public CurrencyManager(@NotNull BukkitPlatform platform, @NotNull MessageConfig messageConfig) {
        this.platform = platform;
        this.messageConfig = messageConfig;
    }

    @Nullable
    public Currency getCurrency(@NotNull String id) {
        return currencies.get(id);
    }

    @NotNull
    public List<String> getAllCurrencyIds() {
        return List.copyOf(currencies.keySet());
    }

    @NotNull
    public Map<String, Currency> getCurrencies() {
        return currencies;
    }

    public void registerCurrency(@NotNull Currency currency) {
        if (currencies.containsKey(currency.id())) {
            throw new IllegalArgumentException("Currency with id " + currency.id() + " is already registered.");
        }

        currencies.put(currency.id(), currency);
        log.debug("Registered currency: {}", currency.id());

        platform.getInternalCommandManager().registerCurrencyCommand(currency);
    }
}

/*
 * Copyright (c) 2026.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.util;

import gg.nerdzone.prison.economy.EconomyAPI;
import gg.nerdzone.prison.economy.EconomyType;
import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.api.context.state.MineContextState;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.lucko.helper.Services;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
@Slf4j
public class MineCurrencyUtil {

    private static final EconomyAPI ECONOMY_API = Services.load(EconomyAPI.class);

    public void registerCurrencyComplete(@NotNull MineBlockBreakContext breakContext, @NotNull EconomyType economyType) {
        registerCurrencyComplete(breakContext, economyType, null);
    }

    public void registerCurrencyComplete(@NotNull MineBlockBreakContext breakContext,
                                         @NotNull EconomyType economyType,
                                         @Nullable UnaryOperator<Double> formula) {
        breakContext.getState().whenComplete(
            "currency-%s".formatted(economyType.name().toLowerCase()),
            MineContextState.StatePriority.MONITOR,
            (context) -> {
                final double blocksCount = context.getBlocksCount();
                if (blocksCount <= 0) {
                    return;
                }

                final double amount = Objects.requireNonNullElse(formula, UnaryOperator.<Double>identity()).apply(blocksCount);
                final Player player = context.getPlayer();
                if (player == null) {
                    return;
                }

                handleEnchantCurrencies(player, Map.of(economyType, amount));
            }
        );
    }

    public void handleEnchantCurrencies(Player player, Map<EconomyType, Double> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return;
        }

        currencies.forEach((economyType, amount) -> {
            try {
                ECONOMY_API.add(player.getName(), economyType, amount);
            } catch (Exception exception) {
                log.error("Failed to deposit enchant economy: {} {} to player {}", amount, economyType, player.getName(), exception);
            }
        });
    }

}

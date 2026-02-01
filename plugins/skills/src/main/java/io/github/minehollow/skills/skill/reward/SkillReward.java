package io.github.minehollow.skills.skill.reward;

import io.github.minehollow.minecraft.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public sealed interface SkillReward {

    static SkillReward common(@NotNull String description) {
        return new CommonSkill(description);
    }

    static SkillReward currency(@NotNull Currency currency, double amount) {
        return new CurrencyReward(currency, amount);
    }


    @NotNull String description();

    default void giveToPlayer(@NotNull Player player) {
        // Default implementation does nothing
    }


    record CommonSkill(@NotNull String description) implements SkillReward {
    }

    record GlobalExperienceReward(double amount) implements SkillReward {
        @Override
        public @NotNull String description() {
            return String.format("%.2f Pontos de Ranking", amount);
        }

        @Override
        public void giveToPlayer(@NotNull Player player) {
            Bukkit.dispatchCommand(
              Bukkit.getConsoleSender(),
              "gexp give " + player.getName() + " " + amount
            );
        }
    }


    record CurrencyReward(@NotNull Currency currency, double amount) implements SkillReward {
        @Override
        public @NotNull String description() {
            return currency.formatAmount(new BigDecimal(amount));
        }

        @Override
        public void giveToPlayer(@NotNull Player player) {
            final var currencyId = currency.id();
            Bukkit.dispatchCommand(
              Bukkit.getConsoleSender(),
              "eco give " + player.getName() + " " + currencyId + " " + amount
            );
        }
    }
}

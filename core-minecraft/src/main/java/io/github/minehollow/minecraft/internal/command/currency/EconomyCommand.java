package io.github.minehollow.minecraft.internal.command.currency;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.util.message.MessageConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.wallet.WalletTransactionContext;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class EconomyCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public EconomyCommand(@NotNull BukkitPlatform platform) {
        super("economy");
        this.platform = platform;
        this.setAliases(List.of("eco"));
        this.setRequiredPermission("economy.admin");
    }

    private MessageConfig messages() {
        return platform.getMessageConfig();
    }

    /**
     * Returns a MiniMessage Component for the given currency and key.
     */
    private Component msg(@NotNull String currencyId, @NotNull String key, @NotNull Object... replacements) {
        return messages().get("currency-messages", key, replacements);
    }

    /**
     * Returns a raw MiniMessage string for use in CommandFailedException
     * and getRawArgOrThrow error messages (parsed by SimpleCommand).
     */
    private String rawMsg(@NotNull String currencyId, @NotNull String key, @NotNull Object... replacements) {
        return messages().getRaw("currency-messages", key, replacements);
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var sub = ctx.getRawArgOrThrow(0, rawMsg("_default", "eco-usage"));
        final var targetName = ctx.getRawArgOrThrow(1, rawMsg("_default", "eco-player-required"));
        final var currencyId = ctx.getRawArgOrThrow(2, rawMsg("_default", "eco-currency-required"));
        final var amountStr = ctx.getRawArgOrThrow(3, rawMsg("_default", "eco-amount-required"));

        Currency currency = platform.getCurrencyManager().getCurrency(currencyId);
        if (currency == null) {
            throw new CommandFailedException(rawMsg("_default", "eco-invalid-currency", "currency", currencyId));
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new CommandFailedException(rawMsg(currencyId, "eco-invalid-amount"));
        }

        Tasks.runAsync(() -> {
            try {
                var wallet = platform.getWalletService().getOrLoadWallet(targetName);
                if (wallet == null) {
                    ctx.sendMessage(msg(currencyId, "eco-player-not-found"));
                    return;
                }

                final var context = WalletTransactionContext.builder()
                    .withInitiatorId(null)
                    .withTargetId(wallet.uniqueId())
                    .withReason("admin_command: " + sub)
                    .build();

                switch (sub.toLowerCase()) {
                    case "add" -> platform.getPlayerWalletService().addCurrencyValue(wallet.uniqueId(), currencyId, amount, context);
                    case "remove" -> platform.getPlayerWalletService().subtractCurrencyValue(wallet.uniqueId(), currencyId, amount, context);
                    case "set" -> platform.getPlayerWalletService().setCurrencyValue(wallet.uniqueId(), currencyId, amount, context);
                    default -> {
                        ctx.sendMessage(msg(currencyId, "eco-invalid-sub", "sub", sub));
                        return;
                    }
                }
                ctx.sendMessage(msg(currencyId, "eco-success", "target", targetName));
            } catch (Exception e) {
                ctx.sendMessage(msg(currencyId, "eco-error"));
                log.error("Erro ao processar comando econômico", e);
            }
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) {
            return List.of("add", "remove", "set");
        }

        if (ctx.isArgsLength(2)) {
            String arg = ctx.getRawArgOrNull(1);
            return Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .filter(n -> arg == null || n.toLowerCase().startsWith(arg.toLowerCase()))
                .toList();
        }

        if (ctx.isArgsLength(3)) {
            return platform.getCurrencyManager().getAllCurrencyIds();
        }
        return List.of();
    }
}
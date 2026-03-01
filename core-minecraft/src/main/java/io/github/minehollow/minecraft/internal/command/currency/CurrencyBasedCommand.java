package io.github.minehollow.minecraft.internal.command.currency;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.util.message.MessageConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.Cooldown;
import io.github.minehollow.minecraft.util.message.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class  CurrencyBasedCommand extends SimpleCommand implements Listener {

    private final BukkitPlatform platform;
    private final Currency currency;

    public CurrencyBasedCommand(BukkitPlatform platform, Currency currency, boolean allowPayments) {
        super(currency.commandName());
        this.platform = platform;
        this.currency = currency;
        Bukkit.getPluginManager().registerEvents(this, platform.getPlugin());
    }

    private MessageConfig messages() {
        return platform.getMessageConfig();
    }

    /**
     * Returns a MiniMessage Component for the given key and placeholders.
     */
    private Component msg(@NotNull String key, @NotNull Object... replacements) {
        return messages().get("currency-messages", key, replacements);
    }

    /**
     * Returns a raw MiniMessage string (pre-parsed) for use in CommandFailedException
     * and getRawArgOrThrow error messages (which are parsed by SimpleCommand).
     */
    private String rawMsg(@NotNull String key, @NotNull Object... replacements) {
        return messages().getRaw("currency-messages", key, replacements);
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) {
            Player p = ctx.getSenderAsPlayer();
            BigDecimal bal = platform.getWalletService().getCachedWalletOrThrow(p.getUniqueId()).getCurrencyAmount(currency.id());
            ctx.sendMessage(msg("balance-self",
              "symbol", currency.symbol(),
              "balance", currency.formatAmountSimple(bal),
              "currency", currency.displayName(),
              "currency_plural", currency.pluralDisplayName()
            ));
            return;
        }

        switch (sub.toLowerCase()) {
            case "ver" -> handleViewBalance(ctx, ctx.getRawArgOrThrow(1, rawMsg("eco-player-required")));
            case "pagar", "pay" -> {
                if (!currency.allowPlayerPayments()) throw new CommandFailedException(rawMsg("pay-disabled"));
                handlePay(ctx, ctx.getRawArgOrThrow(1, rawMsg("eco-player-required")), ctx.getRawArgOrThrow(2, rawMsg("eco-amount-required")));
            }
            case "top" -> handleTop(ctx);
            default -> throw new CommandFailedException(rawMsg("usage"));
        }
    }

    private void handleViewBalance(CommandContext ctx, String targetName) {
        Tasks.runAsync(() -> {
            try {
                var wallet = platform.getWalletService().getOrLoadWallet(targetName);
                if (wallet == null) {
                    ctx.sendMessage(msg("balance-other-error"));
                    return;
                }
                ctx.sendMessage(msg("balance-other",
                  "target", targetName,
                  "symbol", currency.symbol(),
                  "balance", currency.formatAmountSimple(wallet.getCurrencyAmount(currency.id())),
                  "currency", currency.displayName(),
                  "currency_plural", currency.pluralDisplayName()
                ));
            } catch (Exception e) {
                ctx.sendMessage(msg("balance-other-fetch-error"));
            }
        });
    }

    private void handlePay(CommandContext ctx, String targetName, String amountStr) {
        Player sender = ctx.getSenderAsPlayer();
        if (sender.getName().equalsIgnoreCase(targetName)) {
            throw new CommandFailedException(rawMsg("pay-self"));
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) throw new CommandFailedException(rawMsg("pay-target-offline"));

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new CommandFailedException(rawMsg("pay-invalid-amount"));
        }

        if (!Cooldown.setIfNotInCooldown(sender.getUniqueId(), 5000L, "pay-" + currency.id())) {
            throw new CommandFailedException(rawMsg("pay-cooldown"));
        }

        Tasks.runAsync(() -> {
            try {
                var source = platform.getWalletService().getCachedWallet(sender.getUniqueId());
                if (source == null || !source.hasAmount(currency.id(), amount)) {
                    ctx.sendMessage(msg("pay-insufficient-balance"));
                    return;
                }

                final var updatedSource = platform.getWalletService().decrementCurrencyValue(sender.getUniqueId(), currency.id(), amount, true);
                final var targetSource = platform.getWalletService().incrementCurrencyValue(target.getUniqueId(), currency.id(), amount, true);

                ctx.sendMessage(msg("pay-sent",
                  "symbol", currency.symbol(),
                  "amount", currency.formatAmountSimple(amount),
                  "target", target.getName(),
                  "balance", currency.formatAmountSimple(updatedSource.getCurrencyAmount(currency.id())),
                  "currency", currency.displayName(),
                  "currency_plural", currency.pluralDisplayName()
                ));

                messages().send(target, "currency-messages", "pay-received",
                  "symbol", currency.symbol(),
                  "amount", currency.formatAmountSimple(amount),
                  "player", sender.getName(),
                  "balance", currency.formatAmountSimple(targetSource.getCurrencyAmount(currency.id())),
                  "currency", currency.displayName(),
                  "currency_plural", currency.pluralDisplayName()
                );

                Cooldown.removeCooldown(sender.getUniqueId(), "pay-" + currency.id());
            } catch (Exception e) {
                log.error("Erro no pagamento", e);
                ctx.sendMessage(msg("pay-error"));
            }
        });
    }

    private void handleTop(CommandContext ctx) {
        Tasks.runAsync(() -> {
            var top = platform.getWalletService().getTopWalletsByCurrency(currency.id(), 10);
            if (top.isEmpty()) {
                ctx.sendMessage(msg("top-empty"));
                return;
            }

            ctx.sendMessage(msg("top-header",
              "currency", currency.displayName(),
              "currency_plural", currency.pluralDisplayName(),
              "symbol", currency.symbol()
            ));
            for (int i = 0; i < top.size(); i++) {
                var e = top.get(i);
                ctx.sendMessage(msg("top-entry",
                  "index", (i + 1),
                  "player", e.playerName(),
                  "amount", currency.formatAmount(e.amount()),
                  "symbol", currency.symbol(),
                  "currency", currency.displayName(),
                  "currency_plural", currency.pluralDisplayName()
                ));
            }

            if (ctx.getSender() instanceof Player p) {
                long pos = platform.getWalletService().getWalletPositionInCurrencyRanking(p.getUniqueId(), currency.id());
                ctx.sendMessage(msg("top-position", "position", pos));
            }
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final String arg0 = ctx.getRawArgOrNull(0);
        if (ctx.isArgsLength(1))
            return Stream.of("ver", "pagar", "top")
              .filter(s -> arg0 == null || s.startsWith(arg0))
              .toList();

        final String sub = arg0 == null ? "" : arg0.toLowerCase();
        if ((sub.equals("ver") || sub.equals("pagar") || sub.equals("pay")) && ctx.isArgsLength(2)) {
            String arg1 = ctx.getRawArgOrNull(1);
            return Bukkit.getOnlinePlayers()
              .stream()
              .map(Player::getName)
              .filter(n -> filter(ctx, n, arg1)).toList();
        }

        return List.of();
    }

    private boolean filter(@NotNull CommandContext ctx, String n, String arg1) {
        return !n.equalsIgnoreCase(ctx.getSender().getName()) && (arg1 == null || n.toLowerCase().startsWith(arg1.toLowerCase()));
    }
}
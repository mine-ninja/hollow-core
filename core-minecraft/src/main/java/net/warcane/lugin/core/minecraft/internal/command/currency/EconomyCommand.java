package net.warcane.lugin.core.minecraft.internal.command.currency;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.currency.Currency;
import net.warcane.lugin.core.player.wallet.WalletService;
import net.warcane.lugin.core.player.wallet.log.WalletBalanceLog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EconomyCommand extends SimpleCommand {

    private final BukkitPlatform platform;
    private final WalletService walletService;

    public EconomyCommand(BukkitPlatform platform) {
        super("economy", "lugin.master");
        this.setAliases(List.of("eco"));
        this.platform = platform;
        this.walletService = platform.getWalletService();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            ctx.sendMessage("§cVocê deve especificar um subcomando: view, add, remove, set, list");
            return;
        }

        if (subCommand.equalsIgnoreCase("list")) {
            handleListCurrencies(ctx);
            return;
        }

        final var playerName = ctx.getRawArgOrThrow(1, "§cEspecifique o nome do jogador");
        final var currencyId = ctx.getRawArgOrThrow(2, "§cEspecifique o ID da moeda");
        final var currency = platform.getCurrencyManager().getCurrency(currencyId);

        if (currency == null) {
            ctx.sendMessage("§cMoeda inválida: " + currencyId);
            return;
        }

        switch (subCommand.toLowerCase()) {
            case "view" -> handleView(ctx, playerName, currency);
            case "add" -> handleOperation(ctx, playerName, currency, BalanceOperation.ADD);
            case "remove" -> handleOperation(ctx, playerName, currency, BalanceOperation.REMOVE);
            case "set" -> handleOperation(ctx, playerName, currency, BalanceOperation.SET);
            default -> ctx.sendMessage("§cSubcomando inválido");
        }
    }

    private void handleView(CommandContext ctx, String playerName, Currency currency) {
        walletService.getOrLoadWallet(playerName).whenComplete((wallet, error) -> {
            if (error != null || wallet == null) {
                ctx.sendMessage("§cCarteira não encontrada");
                return;
            }

            final var amount = wallet.getCurrencyAmount(currency.id());
            final var formatted = currency.formatAmount(amount);
            ctx.sendMessage("§aSaldo de §b" + playerName + "§a: §b" + formatted);
        });
    }

    private void handleOperation(CommandContext ctx, String playerName, Currency currency, BalanceOperation op) {
        final var amount = ctx.getBigDecimalOrThrow(3, "§cEspecifique a quantidade");

        walletService.getOrLoadWallet(playerName).whenComplete((wallet, error) -> {
            if (error != null || wallet == null) {
                ctx.sendMessage("§cCarteira não encontrada");
                return;
            }

            final var modified = switch (op) {
                case ADD -> wallet.addCurrencyAmount(currency.id(), amount);
                case REMOVE -> wallet.subtractCurrencyAmount(currency.id(), amount);
                case SET -> wallet.updateCurrencyAmount(currency.id(), amount);
            };

            final var logType = switch (op) {
                case ADD -> WalletBalanceLog.WalletBalanceLogType.ADDITION;
                case REMOVE -> WalletBalanceLog.WalletBalanceLogType.SUBTRACTION;
                case SET -> WalletBalanceLog.WalletBalanceLogType.UPDATE;
            };

            final var log = WalletBalanceLog.createLog(wallet.uniqueId(), currency.id(), amount, logType);
            walletService.addLogToContainer(wallet.uniqueId(), log);

            walletService.saveWallet(modified).whenComplete((saved, saveError) -> {
                if (saveError != null) {
                    ctx.sendMessage("§cErro ao salvar carteira");
                    return;
                }

                final var newAmount = saved.getCurrencyAmount(currency.id());
                final var formatted = currency.formatAmount(newAmount);
                ctx.sendMessage("§aOperação realizada! Novo saldo: §b" + formatted);
            });
        });
    }

    private void handleListCurrencies(CommandContext ctx) {
        final var currencies = platform.getCurrencyManager().getCurrencies().values();
        if (currencies.isEmpty()) {
            ctx.sendMessage("§cNenhuma moeda registrada");
            return;
        }

        ctx.sendMessage("§aMoedas registradas:");
        currencies.forEach(currency -> {
            ctx.sendMessage(" §7- §b" + currency.id() + "§7: " + currency.displayName());
        });
    }

    enum BalanceOperation {
        ADD, REMOVE, SET
    }
}
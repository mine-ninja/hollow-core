package net.warcane.lugin.core.minecraft.internal.command.currency;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.currency.Currency;
import net.warcane.lugin.core.minecraft.util.version.VersionChecker;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult.Failure;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult.InsufficientFunds;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult.InvalidCurrency;
import net.warcane.lugin.core.player.wallet.transaction.TransactionResult.WalletNotFound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class CurrencyBasedCommand extends SimpleCommand {

    private static final Pattern BIG_DECIMAL_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final List<String> INVALID_AMOUNT_TOKENS = List.of("nan", "inf", "-inf", "null", "undefined");

    private final BukkitPlatform platform;
    private final Currency currency;

    public CurrencyBasedCommand(BukkitPlatform platform, Currency currency) {
        super(currency.commandName());
        this.platform = platform;
        this.currency = currency;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            final var sender = ctx.getSender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage("§cEste comando só pode ser usado por jogadores.");
                return;
            }
            final var balance = platform.getWalletService().getCachedWalletOrThrow(player.getUniqueId()).getCurrencyAmount(currency.id());
            ctx.sendMessage("§aVocê tem §b" + currency.formatAmount(balance) + "§a.");
            return;
        }

        switch (subCommand.toLowerCase()) {
            case "ver" ->
              handleViewBalanceCommand(ctx, ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador."));
            case "pagar", "pay" -> {
                if (currency.allowPlayerPayments()) {
                    ctx.sendMessage("§cEste comando não está disponível para pagamentos de jogadores.");
                    return;
                }
                handlePayCommand(ctx, ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador."));
            }
            case "top" -> handleTopCommand(ctx);
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: ver, pagar ou top.");
        }
    }

    private void handleViewBalanceCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        platform.getWalletService().getOrLoadWallet(playerName).whenComplete((found, error) -> {
            if (error != null || found == null) {
                ctx.sendMessage(error != null ? "§cErro ao buscar o saldo do jogador: " + error.getMessage() : "§cJogador não encontrado ou não possui uma carteira.");
                return;
            }
            ctx.sendMessage("§aO saldo de §b" + playerName + "§a é §b" + currency.formatAmount(found.getCurrencyAmount(currency.id())) + "§a.");
        });
    }

    private void handlePayCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        final var targetWallet = platform.getWalletService().getCachedWallet(playerName);
        if (targetWallet == null)
            throw new CommandFailedException("§cJogador não encontrado ou não possui uma carteira.");

        final var sender = ctx.getSender();
        if (!(sender instanceof Player player))
            throw new CommandFailedException("§cEste comando só pode ser usado por jogadores.");

        final var sourceWallet = platform.getWalletService().getCachedWallet(player.getUniqueId());
        if (sourceWallet == null)
            throw new CommandFailedException("§cVocê não possui uma carteira para realizar pagamentos.");

        final var amountToPay = ctx.getRawArgOrThrow(2, "§cEspecifique uma quantia para pagar");
        if (!BIG_DECIMAL_PATTERN.matcher(amountToPay).matches() || amountToPay.startsWith("-") || INVALID_AMOUNT_TOKENS.contains(amountToPay.toLowerCase())) {
            throw new CommandFailedException("§cQuantia inválida. Use um número válido.");
        }

        final var amount = new BigDecimal(amountToPay);
        if (!sourceWallet.hasAmount(currency.id(), amount)) {
            throw new CommandFailedException("§cVocê não possui saldo suficiente para pagar " + currency.formatAmount(amount) + ".");
        }

        platform.getWalletService().transferCurrency(
          sourceWallet.uniqueId(),
          targetWallet.uniqueId(),
          currency.id(),
          amount
        ).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Erro ao transferir moeda: ", error);
                ctx.sendMessage("§cErro ao realizar o pagamento: " + error.getMessage());
                return;
            }

            switch (result) {
                case TransactionResult.Success ignored -> {
                    final var bukkitSound = VersionChecker.isModernVersion()
                      ? Sound.BLOCK_NOTE_BLOCK_PLING
                      : Sound.valueOf("NOTE_PLING");

                    player.playSound(player.getLocation(), bukkitSound, 1.0f, 1.0f);
                    ctx.sendMessage("§aPagamento de " + currency.formatAmount(amount) + " realizado com sucesso para " + playerName + ".");
                    platform.sendMessageToPlayer(targetWallet.uniqueId(), "§aVocê recebeu um pagamento de %s de %s.".formatted(currency.formatAmount(amount), player.getName()));
                }

                case Failure(Throwable throwable) ->
                  throw new CommandFailedException("§cErro ao realizar o pagamento: " + throwable);
                case InsufficientFunds(String currencyId, BigDecimal requiredAmount, BigDecimal providedAmount) ->
                  throw new CommandFailedException("§cSaldo insuficiente para pagar " + currency.formatAmount(requiredAmount) + ". §cVocê tem apenas " + currency.formatAmount(providedAmount) + ".");
                case InvalidCurrency ignored ->
                  throw new CommandFailedException("§cMoeda inválida especificada para o pagamento.");
                case WalletNotFound ignored ->
                  throw new CommandFailedException("§cCarteira do jogador não encontrada.");
                case null, default -> throw new CommandFailedException("§cErro desconhecido ao realizar o pagamento.");
            }
        });
    }

    private void handleTopCommand(@NotNull CommandContext ctx) {
        ctx.sendMessage("§cComando 'top' ainda não implementado.");
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return ctx.isArgsLength(0) || ctx.isArgsLength(1) ? List.of() : super.performTabComplete(ctx);
    }
}
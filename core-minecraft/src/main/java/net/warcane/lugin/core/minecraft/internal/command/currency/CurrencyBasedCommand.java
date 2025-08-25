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
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
            ctx.sendMessage("§aVocê tem §b" + currency.formatAmountSimple(balance) + "§a.");
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

                this.handlePayCommand(ctx, ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador para pagar."));
            }
            case "top" -> handleTopCommand(ctx);
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: ver, pagar ou top.");
        }
    }

    private void handleViewBalanceCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        ctx.sendMessage("§7§oCarregando informações...");

        platform.getWalletService()
          .getOrLoadWallet(playerName)
          .orTimeout(3, TimeUnit.SECONDS)
          .whenComplete((found, error) -> {
              if (error != null) {
                  log.error("Erro ao buscar o saldo do jogador {}: {}", playerName, error.getMessage());
                  return;
              }

              if (found == null) {
                  ctx.sendMessage("§cUm erro ocorreu ao buscar o saldo do jogador " + playerName + ". O jogador pode não ter uma carteira.");
                return;
              }

              ctx.sendMessage("§aO saldo de §b" + playerName + "§a é §b" + currency.formatAmount(found.getCurrencyAmount(currency.id())) + "§a.");
          });
    }

    private void handlePayCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        if (ctx.getSenderAsPlayer().getName().equalsIgnoreCase(playerName)) {
            throw new CommandFailedException("§cVocê não pode pagar a si mesmo.");
        }

        final var targetWallet = platform.getWalletService().getCachedWallet(playerName);
        if (targetWallet == null)
            throw new CommandFailedException("§cJogador não encontrado ou não possui uma carteira.");

        final var localTargetPlayer = Bukkit.getPlayer(playerName);
        if (localTargetPlayer == null || !localTargetPlayer.isOnline()) {
            throw new CommandFailedException("§cO jogador " + playerName + " não está online no mesmo servidor que você.");
        }

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
        // checkar se é zero ou menor antes...
        if (amount.equals(BigDecimal.ZERO) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommandFailedException("§cVocê não pode pagar uma quantia menor ou igual a zero.");
        }

        if (!sourceWallet.hasAmount(currency.id(), amount)) {
            throw new CommandFailedException("§cVocê não possui saldo suficiente para pagar " + currency.formatAmount(amount) + ".");
        }

        platform.getExecutorService().execute(() -> {
            TransactionResult result = platform.getWalletService()
              .transferCurrency(sourceWallet.uniqueId(), targetWallet.uniqueId(), currency.id(), amount);

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
        platform.getWalletService()
          .getTopWalletsByCurrencyBalance(currency.id(), 10)
          .whenCompleteAsync((list, error) -> {
              if (error != null) {
                  error.printStackTrace();
                  log.error("Erro ao buscar o top de carteiras: ", error);
                  ctx.sendMessage("§cUm erro interno ocorreu ao obter o top, tente novamente mais tarde.");
                  return;
              }

              if (list.isEmpty()) {
                  ctx.sendMessage("§cNenhum jogador encontrado com saldo na moeda " + currency.id() + ".");
                  return;
              }

              ctx.sendMessage("§aJogadores mais ricos em " + currency.pluralDisplayName() + ":");
              for (int i = 0; i < list.size(); i++) {
                  final var wallet = list.get(i);
                  ctx.sendMessage("§e" + (i + 1) + ". §b" + wallet.playerName() + "§a - " + currency.formatAmount(wallet.balance()));
              }
          });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(0) || ctx.isArgsLength(1)) {
            final var input = ctx.getRawArgOrNull(0);
            final var subcommands = List.of("ver", "pagar", "pay", "top");

            if (input == null) {
                return subcommands;
            }

            return subcommands.stream()
              .filter(cmd -> cmd.toLowerCase().startsWith(input.toLowerCase()))
              .toList();
        }

        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            return List.of();
        }

        switch (subCommand.toLowerCase()) {
            case "ver" -> {
                if (ctx.isArgsLength(2)) {
                    final var input = ctx.getRawArgOrNull(1);
                    return Bukkit.getOnlinePlayers().stream()
                      .map(Player::getName)
                      .filter(name -> input == null || name.toLowerCase().startsWith(input.toLowerCase()))
                      .sorted()
                      .toList();
                }
            }

            case "pagar", "pay" -> {
                if (!currency.allowPlayerPayments()) {
                    return List.of();
                }

                if (ctx.isArgsLength(2)) {
                    final var input = ctx.getRawArgOrNull(1);
                    final var sender = ctx.getSender();
                    final var senderName = sender instanceof Player player ? player.getName() : null;

                    return Bukkit.getOnlinePlayers().stream()
                      .map(Player::getName)
                      .filter(name -> !name.equals(senderName)) // Excluir o próprio jogador
                      .filter(name -> input == null || name.toLowerCase().startsWith(input.toLowerCase()))
                      .sorted()
                      .toList();
                } else if (ctx.isArgsLength(3)) {
                    // Terceiro argumento: sugerir algumas quantias comuns
                    final var input = ctx.getRawArgOrNull(2);
                    final var suggestions = List.of("1", "5", "10", "50", "100", "500", "1000");

                    if (input == null || input.isEmpty()) {
                        return suggestions;
                    }

                    if (BIG_DECIMAL_PATTERN.matcher(input).matches()) {
                        return List.of();
                    }

                    return suggestions.stream()
                      .filter(suggestion -> suggestion.startsWith(input))
                      .toList();
                }
            }

            case "top" -> {
                return List.of();
            }
        }

        return List.of();
    }
}

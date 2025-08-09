package net.warcane.lugin.core.minecraft.internal.command.currency;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.currency.Currency;
import net.warcane.lugin.core.player.wallet.WalletService;
import org.jetbrains.annotations.NotNull;

public class EconomyCommand extends SimpleCommand {

    private static final String SUBCOMMAND_ERROR = "§cVocê deve especificar um subcomando: ver, pagar, adicionar, remover ou listar.";
    private static final String PLAYER_NAME_ERROR = "§cVocê deve especificar o nome do jogador.";
    private static final String CURRENCY_ID_ERROR = "§cVocê deve especificar o ID da moeda.";
    private static final String INVALID_CURRENCY_ERROR = "§cMoeda inválida: %s. Use uma das seguintes: %s";
    private static final String INVALID_SUBCOMMAND_ERROR = "§cSubcomando inválido. Use: set, add, remove, view ou list.";
    private static final String WALLET_ERROR = "§cErro ao buscar a carteira do jogador: %s";
    private static final String PLAYER_NOT_FOUND = "§cJogador não encontrado ou não possui uma carteira.";
    private static final String BALANCE_AMOUNT_ERROR = "§cVocê deve especificar a quantidade a ser %s na carteira do jogador.";
    private static final String UPDATE_WALLET_ERROR = "§cErro ao atualizar a carteira do jogador: %s";
    private static final String VIEW_BALANCE_MESSAGE = "§aO saldo de §b%s§a é §b%s§a.";
    private static final String OPERATION_SUCCESS_MESSAGE = "§aOperação %s realizada com sucesso na carteira de §b%s§a. Novo saldo: §b%s§a.";


    private final BukkitPlatform platform;
    private final WalletService walletService;

    public EconomyCommand(BukkitPlatform platform) {
        super("economy");
        this.requiredPermission = "lugin.master";
        this.platform = platform;
        this.walletService = platform.getWalletService();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            ctx.sendMessage(SUBCOMMAND_ERROR);
            return;
        }

        final var playerName = ctx.getRawArgOrThrow(1, PLAYER_NAME_ERROR);
        final var currencyId = ctx.getRawArgOrThrow(2, CURRENCY_ID_ERROR);
        final var currency = platform.getCurrencyManager().getCurrency(currencyId);
        if (currency == null) throw new CommandFailedException(INVALID_CURRENCY_ERROR.formatted(currencyId, String.join(", ", platform.getCurrencyManager().getAllCurrencyIds())));


        final var commandCtx = new EconomyCommandContext(ctx, playerName, currency);
        switch (subCommand.toLowerCase()) {
            case "set" -> handleBalanceOperation(commandCtx, BalanceOperation.SET);
            case "add" -> handleBalanceOperation(commandCtx, BalanceOperation.ADD);
            case "remove" -> handleBalanceOperation(commandCtx, BalanceOperation.REMOVE);
            case "view" -> handleViewBalanceCommand(commandCtx);
            case "list" -> handleListCurrenciesCommand(ctx);
            default -> throw new CommandFailedException(INVALID_SUBCOMMAND_ERROR);
        }
    }

    private void handleViewBalanceCommand(@NotNull EconomyCommandContext ctx) {
        walletService.getOrLoadWallet(ctx.playerName())
          .whenComplete((found, error) -> {
              if (error != null) {
                  throw new CommandFailedException(WALLET_ERROR.formatted(error.getMessage()));
              }
              if (found == null) {
                  ctx.sendMessage(PLAYER_NOT_FOUND);
                  return;
              }
              final var amount = found.getCurrencyAmount(ctx.currency().id());
              final var formattedAmount = ctx.currency().formatAmount(amount);
              ctx.sendMessage(VIEW_BALANCE_MESSAGE.formatted(ctx.playerName(), formattedAmount));
          });
    }

    private void handleBalanceOperation(@NotNull EconomyCommandContext ctx, @NotNull BalanceOperation operation) {
        final var amount = ctx.commandContext().getBigDecimalOrThrow(
          3,
          BALANCE_AMOUNT_ERROR.formatted(operation.name().toLowerCase())
        );

        walletService.getOrLoadWallet(ctx.playerName())
          .whenComplete((found, error) -> {
              if (error != null) {
                  throw new CommandFailedException(WALLET_ERROR.formatted(error.getMessage()));
              }
              if (found == null) {
                  ctx.sendMessage(PLAYER_NOT_FOUND);
                  return;
              }

              final var changed = switch (operation) {
                  case ADD -> found.addCurrencyAmount(ctx.currency().id(), amount);
                  case REMOVE -> found.subtractCurrencyAmount(ctx.currency().id(), amount);
                  case SET -> found.updateCurrencyAmount(ctx.currency().id(), amount);
              };

              walletService.saveWallet(changed)
                .whenComplete((updated, updateError) -> {
                    if (updateError != null) {
                        throw new CommandFailedException(UPDATE_WALLET_ERROR.formatted(updateError.getMessage()));
                    }

                    final var formattedAmount = ctx.currency().formatAmount(amount);
                    ctx.sendMessage(OPERATION_SUCCESS_MESSAGE.formatted(operation.name().toLowerCase(), ctx.playerName(), formattedAmount));
                });
          });
    }

    private void handleListCurrenciesCommand(@NotNull CommandContext ctx) {
        final var currencies = platform.getCurrencyManager().getCurrencies().values();
        if (currencies.isEmpty()) {
            ctx.sendMessage("§cNenhuma moeda registrada.");
            return;
        }

        ctx.sendMessage("§aMoedas registradas:");
        for (var currency : currencies) {
            ctx.sendMessage(" - §b" + currency.id() + "§a: " + currency.displayName() + " §7(" + currency.symbol() + "§7)");
        }
    }

    private record EconomyCommandContext(
      @NotNull CommandContext commandContext,
      @NotNull String playerName,
      @NotNull Currency currency
    ) {
        public void sendMessage(@NotNull String... msg) {
            commandContext.sendMessage(msg);
        }
    }

    enum BalanceOperation {
        ADD, REMOVE, SET
    }
}
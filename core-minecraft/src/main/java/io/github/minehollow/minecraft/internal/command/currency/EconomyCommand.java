package io.github.minehollow.minecraft.internal.command.currency;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.task.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class EconomyCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public EconomyCommand(@NotNull BukkitPlatform platform) {
        super("economy");
        this.platform = platform;
        this.setAliases(List.of("eco"));
        this.setRequiredPermission("economy.admin");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var sub = ctx.getRawArgOrThrow(0, "§cUse: /eco <add|remove|set> <jogador> <currency> <quantia>");
        final var targetName = ctx.getRawArgOrThrow(1, "§cEspecifique o jogador.");
        final var currencyId = ctx.getRawArgOrThrow(2, "§cEspecifique a currency.");
        final var amountStr = ctx.getRawArgOrThrow(3, "§cEspecifique a quantia.");


        Currency currency = platform.getCurrencyManager().getCurrency(currencyId);
        if (currency == null) {
            throw new CommandFailedException("§cCurrency inválida: " + currencyId);
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
        } catch (Exception e) {
            throw new CommandFailedException("§cQuantia inválida.");
        }

        Tasks.runAsync(() -> {
            try {
                var wallet = platform.getWalletService().getOrLoadWallet(targetName);
                if (wallet == null) {
                    ctx.sendMessage("§cJogador não encontrado ou sem carteira.");
                    return;
                }

                switch (sub.toLowerCase()) {
                    case "add" ->
                      platform.getWalletService().incrementCurrencyValue(wallet.uniqueId(), currencyId, amount, true);
                    case "remove" ->
                      platform.getWalletService().decrementCurrencyValue(wallet.uniqueId(), currencyId, amount, true);
                    case "set" -> {
                        wallet.setCurrencyAmount(currencyId, amount);
                        platform.getWalletService().updateWallet(wallet);
                    }
                    default -> {
                        ctx.sendMessage("§cSubcomando inválido: " + sub);
                        return;
                    }
                }
                ctx.sendMessage("§aOperação realizada com sucesso para §b" + targetName + "§a.");
            } catch (Exception e) {
                ctx.sendMessage("§cErro ao processar operação econômica.");
                log.error("Erro ao processar comando econômico", e);
            }
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) return List.of("add", "remove", "set");

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
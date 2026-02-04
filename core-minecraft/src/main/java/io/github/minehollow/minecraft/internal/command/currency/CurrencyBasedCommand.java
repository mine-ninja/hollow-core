package io.github.minehollow.minecraft.internal.command.currency;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.Cooldown;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
public class CurrencyBasedCommand extends SimpleCommand implements Listener {

    private final BukkitPlatform platform;
    private final Currency currency;
    private final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();

    public CurrencyBasedCommand(BukkitPlatform platform, Currency currency, boolean allowPayments) {
        super(currency.commandName());
        this.platform = platform;
        this.currency = currency;
        Bukkit.getPluginManager().registerEvents(this, platform.getPlugin());
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        cooldownMap.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) {
            Player p = ctx.getSenderAsPlayer();
            BigDecimal bal = platform.getWalletService().getCachedWalletOrThrow(p.getUniqueId()).getCurrencyAmount(currency.id());
            ctx.sendMessage("§fVocê tem §a" + currency.formatAmountSimple(bal) + "§f.");
            return;
        }

        switch (sub.toLowerCase()) {
            case "ver" -> handleViewBalance(ctx, ctx.getRawArgOrThrow(1, "§cEspecifique o jogador."));
            case "pagar", "pay" -> {
                if (!currency.allowPlayerPayments()) throw new CommandFailedException("§cPagamentos desabilitados.");
                handlePay(ctx, ctx.getRawArgOrThrow(1, "§cEspecifique o jogador."), ctx.getRawArgOrThrow(2, "§cEspecifique a quantia."));
            }
            case "top" -> handleTop(ctx);
            default -> throw new CommandFailedException("§cUse: ver, pagar ou top.");
        }
    }

    private void handleViewBalance(CommandContext ctx, String targetName) {
        Tasks.runAsync(() -> {
            try {
                var wallet = platform.getWalletService().getOrLoadWallet(targetName);
                if (wallet == null) {
                    ctx.sendMessage("§cJogador não encontrado.");
                    return;
                }
                ctx.sendMessage("§fSaldo de §a" + targetName + "§f: §a" + currency.formatAmountSimple(wallet.getCurrencyAmount(currency.id())));
            } catch (Exception e) {
                ctx.sendMessage("§cErro ao buscar saldo.");
            }
        });
    }

    private void handlePay(CommandContext ctx, String targetName, String amountStr) {
        Player sender = ctx.getSenderAsPlayer();
        if (sender.getName().equalsIgnoreCase(targetName)) {
            throw new CommandFailedException("§cVocê não pode pagar a si mesmo.");
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) throw new CommandFailedException("§cJogador offline.");

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new Exception("§cQuantia inválida.");
            }
        } catch (Exception e) {
            throw new CommandFailedException("§cQuantia inválida.");
        }

        if (!Cooldown.setIfNotInCooldown(sender.getUniqueId(), 5000L, "pay-" + currency.id())) {
            throw new CommandFailedException("§cAguarde para pagar novamente.");
        }

        Tasks.runAsync(() -> {
            try {
                var source = platform.getWalletService().getCachedWallet(sender.getUniqueId());
                if (source == null || !source.hasAmount(currency.id(), amount)) {
                    ctx.sendMessage("§cSaldo insuficiente.");
                    return;
                }

                final var updatedSource = platform.getWalletService().decrementCurrencyValue(sender.getUniqueId(), currency.id(), amount, true);
                final var targetSource = platform.getWalletService().incrementCurrencyValue(target.getUniqueId(), currency.id(), amount, true);

                ctx.sendMessage(
                  "§fVocê pagou §a" + currency.formatAmountSimple(amount) + " §fa §a" + target.getName() + "§f. " +
                  "Seu novo saldo é §a" + currency.formatAmountSimple(updatedSource.getCurrencyAmount(currency.id())) + "§f."
                );

                target.sendMessage(
                  "§fVocê recebeu §a" + currency.formatAmountSimple(amount) + " §fde §a" + sender.getName() + "§f. " +
                  "Seu novo saldo é §a" + currency.formatAmountSimple(targetSource.getCurrencyAmount(currency.id())) + "§f."
                );

                Cooldown.removeCooldown(sender.getUniqueId(), "pay-" + currency.id());
            } catch (Exception e) {
                log.error("Erro no pagamento", e);
                ctx.sendMessage("§cErro interno na transação.");
            }
        });
    }

    private void handleTop(CommandContext ctx) {
        Tasks.runAsync(() -> {
            var top = platform.getWalletService().getTopWalletsByCurrency(currency.id(), 10);
            if (top.isEmpty()) {
                ctx.sendMessage("§cRanking vazio.");
                return;
            }

            ctx.sendMessage("§6Ranking " + currency.pluralDisplayName() + ":");
            for (int i = 0; i < top.size(); i++) {
                var e = top.get(i);
                ctx.sendMessage("§e" + (i + 1) + ". §f" + e.playerName() + " §7- §f" + currency.formatAmount(e.amount()));
            }

            if (ctx.getSender() instanceof Player p) {
                long pos = platform.getWalletService().getWalletPositionInCurrencyRanking(p.getUniqueId(), currency.id());
                ctx.sendMessage("§fSua posição: §a#" + pos);
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
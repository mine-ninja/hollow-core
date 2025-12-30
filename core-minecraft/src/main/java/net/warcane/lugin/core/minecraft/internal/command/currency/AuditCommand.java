package net.warcane.lugin.core.minecraft.internal.command.currency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.player.wallet.WalletService;
import net.warcane.lugin.core.player.wallet.log.WalletBalanceLog;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AuditCommand extends SimpleCommand {

    private static final int LOGS_PER_PAGE = 8;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm");
    private final WalletService walletService;

    public AuditCommand(BukkitPlatform platform) {
        super("audit", "lugin.master");
        this.setAliases(List.of("auditoria"));
        this.walletService = platform.getWalletService();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        // /audit <Player> <moeda ou "all"> [pagina]
        final var playerName = ctx.getRawArgOrThrow(0, "§cUso: /audit <jogador> <moeda|all> [página]");
        final var currencyArg = ctx.getRawArgOrThrow(1, "§cUso: /audit <jogador> <moeda|all> [página]");
        final var page = ctx.getIntOrDefault(2, 1);

        // Se for "all", não filtra por moeda
        final var currencyFilter = currencyArg.equalsIgnoreCase("all") ? null : currencyArg;

        walletService.getOrLoadWallet(playerName).whenComplete((wallet, error) -> {
            if (error != null || wallet == null) {
                ctx.sendMessage("§cCarteira não encontrada");
                return;
            }

            walletService.getOrLoadLogContainer(wallet.uniqueId()).whenComplete((container, containerError) -> {
                if (containerError != null) {
                    ctx.sendMessage("§cErro ao carregar logs");
                    containerError.printStackTrace();
                    return;
                }

                if (container == null || container.logs().isEmpty()) {
                    ctx.sendMessage("§7Nenhum registro encontrado");
                    return;
                }

                var logs = filterAndSort(container.logs(), currencyFilter);

                if (logs.isEmpty()) {
                    if (currencyFilter != null) {
                        ctx.sendMessage("§7Nenhum registro encontrado para a moeda: §b" + currencyFilter);
                    } else {
                        ctx.sendMessage("§7Nenhum registro encontrado");
                    }
                    return;
                }

                displayPage(ctx, playerName, logs, page, currencyFilter, currencyArg);
            });
        });
    }

    private List<WalletBalanceLog> filterAndSort(List<WalletBalanceLog> logs, String currencyFilter) {
        List<WalletBalanceLog> filtered;

        // Se currencyFilter é null, mostra todos
        if (currencyFilter != null) {
            filtered = logs.stream()
              .filter(log -> log.currencyId().equalsIgnoreCase(currencyFilter))
              .toList();
        } else {
            filtered = new ArrayList<>(logs);
        }

        // Ordena do mais recente para o mais antigo
        filtered = new ArrayList<>(filtered);
        filtered.sort(Comparator.comparing(WalletBalanceLog::timestamp).reversed());

        return filtered;
    }

    private void displayPage(CommandContext ctx, String player, List<WalletBalanceLog> logs, int page, String filter, String displayFilter) {
        final int total = logs.size();
        final int totalPages = (int) Math.ceil((double) total / LOGS_PER_PAGE);
        page = Math.min(Math.max(1, page), totalPages);

        final int start = (page - 1) * LOGS_PER_PAGE;
        final int end = Math.min(start + LOGS_PER_PAGE, total);

        ctx.sendMessage(Component.empty());
        buildHeader(ctx, player, page, totalPages, total, filter);
        ctx.sendMessage(Component.empty());

        for (int i = start; i < end; i++) {
            buildLogLine(ctx, logs.get(i), i + 1);
        }

        ctx.sendMessage(Component.empty());
        buildNav(ctx, player, page, totalPages, displayFilter);
        ctx.sendMessage(Component.empty());
    }

    private void buildHeader(CommandContext ctx, String player, int page, int total, int count, String filter) {
        var line = Component.text("Auditoria", NamedTextColor.WHITE, TextDecoration.BOLD)
          .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
          .append(Component.text(player, NamedTextColor.AQUA));

        if (filter != null) {
            line = line.append(Component.text(" [" + filter + "]", NamedTextColor.YELLOW));
        } else {
            line = line.append(Component.text(" [todas]", NamedTextColor.YELLOW));
        }

        ctx.sendMessage(line);
        ctx.sendMessage(Component.text("Página " + page + "/" + total + " • " + count + " registros", NamedTextColor.GRAY));
    }

    private void buildLogLine(CommandContext ctx, WalletBalanceLog log, int num) {
        var type = switch (log.type()) {
            case ADDITION -> Component.text("ADD", NamedTextColor.GREEN);
            case SUBTRACTION -> Component.text("SUB", NamedTextColor.RED);
            case UPDATE -> Component.text("SET", NamedTextColor.YELLOW);
        };

        var amount = formatAmount(log.amount(), log.type());
        var date = DATE_FORMAT.format(new Date(log.timestamp()));

        var line = Component.text(String.format("#%03d", num), NamedTextColor.DARK_GRAY)
          .append(Component.text("  ", NamedTextColor.WHITE))
          .append(type)
          .append(Component.text("  ", NamedTextColor.WHITE))
          .append(amount)
          .append(Component.text(" ", NamedTextColor.WHITE))
          .append(Component.text(log.currencyId(), NamedTextColor.AQUA))
          .append(Component.text("  ", NamedTextColor.WHITE))
          .append(Component.text(date, NamedTextColor.DARK_GRAY))
          .hoverEvent(HoverEvent.showText(Component.text("Clique para copiar ID", NamedTextColor.YELLOW)))
          .clickEvent(ClickEvent.copyToClipboard(log.logId().toString()));

        ctx.sendMessage(line);
    }

    private Component formatAmount(BigDecimal amount, WalletBalanceLog.WalletBalanceLogType type) {
        String prefix = type == WalletBalanceLog.WalletBalanceLogType.ADDITION ? "+" :
          type == WalletBalanceLog.WalletBalanceLogType.SUBTRACTION ? "-" : "";

        NamedTextColor color = type == WalletBalanceLog.WalletBalanceLogType.ADDITION ? NamedTextColor.GREEN :
          type == WalletBalanceLog.WalletBalanceLogType.SUBTRACTION ? NamedTextColor.RED :
            NamedTextColor.YELLOW;

        return Component.text(prefix + amount.toPlainString(), color);
    }

    private void buildNav(CommandContext ctx, String player, int page, int total, String currencyArg) {
        var nav = Component.empty();

        if (page > 1) {
            nav = nav.append(btn("‹ Anterior", player, page - 1, currencyArg, NamedTextColor.GREEN));
        } else {
            nav = nav.append(Component.text("‹ Anterior", NamedTextColor.DARK_GRAY));
        }

        nav = nav.append(Component.text("    ", NamedTextColor.WHITE));

        int start = Math.max(1, page - 2);
        int end = Math.min(total, page + 2);

        for (int i = start; i <= end; i++) {
            if (i == page) {
                nav = nav.append(Component.text("[" + i + "]", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                nav = nav.append(btn(String.valueOf(i), player, i, currencyArg, NamedTextColor.YELLOW));
            }
            if (i < end) nav = nav.append(Component.text(" ", NamedTextColor.WHITE));
        }

        nav = nav.append(Component.text("    ", NamedTextColor.WHITE));

        if (page < total) {
            nav = nav.append(btn("Próximo ›", player, page + 1, currencyArg, NamedTextColor.GREEN));
        } else {
            nav = nav.append(Component.text("Próximo ›", NamedTextColor.DARK_GRAY));
        }

        ctx.sendMessage(nav);
    }

    private Component btn(String text, String player, int page, String currencyArg, NamedTextColor color) {
        // /audit <player> <moeda|all> [página]
        String cmd = "/audit " + player + " " + currencyArg + " " + page;

        return Component.text(text, color)
          .hoverEvent(HoverEvent.showText(Component.text("Página " + page, NamedTextColor.AQUA)))
          .clickEvent(ClickEvent.runCommand(cmd));
    }
}
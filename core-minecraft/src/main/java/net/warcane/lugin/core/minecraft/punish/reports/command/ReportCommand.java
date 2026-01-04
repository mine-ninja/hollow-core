package net.warcane.lugin.core.minecraft.punish.reports.command;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.reports.ReportManager;
import net.warcane.lugin.core.minecraft.punish.reports.menu.ReportMenu;
import net.warcane.lugin.core.minecraft.util.Cooldown;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.data.PunishmentInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Rok, Pedro Lucas nmm. 03/01/2026
 * @project lugin-core
 */
public class ReportCommand extends SimpleCommand {

    public ReportCommand() {
        super("report");
        setRequiredPermission("lugin.core.report.cmd");
        setAliases(List.of("reportar", "denunciar", "denuncia"));
    }

    @Override
    public void performCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLength(0) || ctx.isArgsLength(2)) {
            throw new CommandFailedException("§cUso correto: /reportar <player> <id-motivo> [link-prova]");
        }

        var player = ctx.getSenderAsPlayer();

        player.sendMessage("§7Procurando...");

        var target = ctx.getRawArgOrNull(0);
        if (target == null) {
            StringUtils.send(player, "<l-error>Ocorreu um erro...");
            return;
        }

        BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccountByName(target).whenComplete((playerAccount, throwable) -> {
            var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
            if (throwable != null) {
                StringUtils.send(audience, "<l-error>Ocorreu um erro ao buscar o jogador: " + throwable.getMessage());
                return;
            }
            if (playerAccount == null) {
                StringUtils.send(audience, "<l-error>Jogador não encontrado.");
                return;
            }

            HashMap<String, Object> reportMenuParams = new HashMap<>();
            reportMenuParams.put(ReportMenu.REPORTED_ACCOUNT_KEY, playerAccount);
            if (ctx.isArgsLength(1)) {
                BukkitPlatform.getInstance().getMenuManager().openToPlayer(player, ReportMenu.class, reportMenuParams);
                return;
            }

            String id = ctx.getRawArgOrNull(1);
            if (!checkReportRequest(player, id, ctx)) {
                return;
            }
            reportMenuParams.put(ReportMenu.REASON_KEY, PunishmentInfo.getPunishmentByModernId(id));

            if (ctx.getRawArgOrNull(2) != null && PunishManager.checkLink(ctx.getRawArgOrNull(2))) {
                reportMenuParams.put(ReportMenu.EVIDENCE_KEY, ctx.getRawArgOrNull(2));
            }
            BukkitPlatform.getInstance().getMenuManager().openToPlayer(player, ReportMenu.class, reportMenuParams);
        });
    }

    private boolean checkReportRequest(Player player, String id, CommandContext ctx) {
        var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        PunishmentInfo punishment;
        try {
            punishment = PunishmentInfo.getPunishmentByModernId(id);
        } catch (IllegalArgumentException ignored) {
            StringUtils.send(audience, "<l-error>Report não encontrada.");
            return false;
        }

        if (!punishment.reportable()) {
            StringUtils.send(audience, "<l-error>Você não pode reportar esse motivo.");
            return false;
        }
        if (ctx.isArgsLength(2)) return true;
        var link = ctx.getRawArgOrNull(2);
        if (!PunishManager.checkLink(link)) {
            StringUtils.send(audience, "<l-error>O link inserido é inválido.");
            return false;
        }
        return true;
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) {
            return BukkitPlatform.getInstance()
                .getPlayerAccountService()
                .getCachedAccounts().stream()
                .map(PlayerAccount::playerName)
                .filter(s -> s.startsWith(ctx.getArgs()[0]))
                .collect(Collectors.toList());
        } else if (ctx.isArgsLength(2)) {
            String prefix = ctx.getArgs()[1].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (PunishmentInfo punishment : PunishmentInfo.PUNISHMENTS) {
                if (!punishment.reportable()) continue;
                if (punishment.title().toLowerCase().startsWith(prefix)) {
                    completions.add(punishment.modernId());
                }
            }
            return completions;
        } else if (ctx.isArgsLength(3)) {
            return StringUtils.matchPartial(ctx.getArgs()[2], List.of("https://", "[link da prova]"));
        } else {
            return new ArrayList<>();
        }
    }
}

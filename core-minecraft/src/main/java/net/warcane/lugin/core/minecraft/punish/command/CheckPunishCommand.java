package net.warcane.lugin.core.minecraft.punish.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.data.*;
import net.warcane.lugin.core.minecraft.util.Tuple;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 * @project punish
 */
public class CheckPunishCommand extends SimpleCommand {
    public CheckPunishCommand() {
        super("checkpunir");
        setRequiredPermission("lugin.helper");
        this.playersOnly = true;
    }

    @Override
    public void performCommand(CommandContext ctx) throws CommandFailedException {
        if (!ctx.isArgsLength(1)) {
            throw new CommandFailedException("§cUso correto: /checkpunir <player>");
        }
        Player player = ctx.getSenderAsPlayer();

        PunishManager.get().getPunishedPlayer(ctx.getRawArgOrThrow(0, "§cJogador não encontrado.")).whenComplete((punished, e) -> {
            Tasks.runAsync(() -> {
                if (e != null) {
                    ctx.getSenderAsPlayer().sendMessage("§cErro ao buscar informações do jogador: " + e.getMessage());
                    return;
                }
                if (punished == null) {
                    player.sendMessage("§cJogador não encontrado ou não possui punições registradas.");
                    return;
                }
                showCheckPunish(player, punished);
            });
        });
    }

    private void showCheckPunish(Player player, PunishedDTO punished) {
        var audience = BukkitPlatform.getInstance().getAdventure().player(player);
        var msg = new ComponentBuilder();
        msg.newLine()
            .simple("§eHistórico de §f" + punished.getName() + "§e:").newLine()
            .newLine();

        for (var punishment : punished.getPunishments()) {
            var info = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());

            var lore = new ArrayList<String>();
            lore.add("§6ID: §7#" + punishment.getId());

            if (player.hasPermission("lugin.admin")) {
                PlayerAccount punisher = BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccount(punishment.getPunisherUuid()).join();
                lore.add("§6IP: §7" + punishment.getIpAddress());
                lore.add("§6Autor: §7" + punisher.playerName());
            }
            lore.add("");

            var punishmentTimeType = info.getPunishment(punishment.getRepeatCount());

            lore.add("§6Motivo: §7" + info.title());
            lore.add("§6Tipo: §7" + punishmentTimeType.b().getTitle());
            lore.add("§6Duração: §7" + punishmentTimeType.a().getTitle());
            lore.add("§6Aplicada em: §7" + punishment.getAppliedAtFormatted());
            // lore.add("§6Início: §7" + punishment.getRepeatCount() + "x"); // TODO
            lore.add("§6Fim: §7" + punishment.getExpiresAtFormatted());
            lore.add("§6Status: " + punishment.getStatus().getModernColor() + punishment.getStatus().getTitle());
            if (punishment.getStatus().equals(PunishmentStatus.REVOKED)) {
                PlayerAccount revoker = BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccount(punishment.getRevokerUuid()).join();
                lore.add("");
                lore.add("§6Revogada em: §7" + punishment.getRevokedAtFormatted());
                lore.add("§6Autor: §7" + revoker.playerName());
                lore.add("§6Motivo: §7" + punishment.getRevokeReason());
            }

            msg.hover(" §7• " + punishment.getStatus().getColor() + info.title(), lore.toArray(new String[0]));

            if (!punishment.getEvidence().startsWith("Não")) {
                msg.linkHover(" §f[Prova]", punishment.getEvidence(), "§7Clique para copiar o link da prova.");
            } else {
                msg.simple(" §f[Sem prova]");
            }

            if (punishment.getRevokerUuid() == null) {
                msg.actionHover(" §f[Revogar]", ClickEvent.suggestCommand("/revogar "  + punishment.getId()), "§7Clique para revogar esta punição.");
            }
            msg.newLine();
        }

        msg.newLine();
        msg.simple("§eLegenda: ");
        msg.hover("§a⬛ Ativa ", "§7O jogador está cumprindo o tempo da punição ainda, portanto, ela está ativa.");
        msg.hover("§e⬛ Pendente ", "§7O jogador ainda não entrou no servidor após a aplicação da punição, portanto, ela está pendente.");
        msg.hover("§c⬛ Finalizada ", "§7O jogador já cumpriu o tempo da punição, portanto, ela já foi finalizada.");
        msg.hover("§7⬛ Revogada ", "§7A punição foi revogada por algum motivo.");
        msg.newLine();
        msg.send(audience);
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
        }
        return List.of();
    }
}

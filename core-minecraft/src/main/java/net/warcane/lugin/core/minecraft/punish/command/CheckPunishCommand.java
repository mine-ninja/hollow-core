package net.warcane.lugin.core.minecraft.punish.command;

import net.kyori.adventure.text.event.ClickEvent;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentInfo;
import net.warcane.lugin.core.punish.data.PunishmentStatus;
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
                    StringUtils.send(player, "<l-red>Jogador não encontrado ou não possui punições registradas.");
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
            .simple("<l-yellow>Histórico de <l-white>" + punished.getName() + "<l-yellow>:").newLine()
            .newLine();

        for (var punishment : punished.getPunishments()) {
            var info = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());

            var lore = new ArrayList<String>();
            lore.add("<l-gold>ID: <l-gray>#" + punishment.getId());

            // Check if the player has permission to view IP
            if (player.hasPermission("lugin.admin")) {
                PlayerAccount punisher = BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccount(punishment.getPunisherUuid()).join();
                lore.add("<l-gold>IP: <l-gray>" + punishment.getIpAddress());
                lore.add("<l-gold>Autor: <l-gray>" + punisher.playerName());
            }
            lore.add("");

            var punishmentTimeType = info.getPunishment(punishment.getRepeatCount());

            lore.add("<l-gold>Motivo: <l-gray>" + info.title());
            lore.add("<l-gold>Tipo: <l-gray>" + punishmentTimeType.b().getTitle());
            lore.add("<l-gold>Duração: <l-gray>" + punishmentTimeType.a().getTitle());
            lore.add("<l-gold>Aplicada em: <l-gray>" + punishment.getAppliedAtFormatted());
            //lore.add("<l-gold>Início: <l-gray>" + punishment.getRepeatCount() + "x"); TODO: Fazer quando tiver pronto
            lore.add("<l-gold>Fim: <l-gray>" + punishment.getExpiresAtFormatted());
            lore.add("<l-gold>Status: <l-gray>" + punishment.getStatus().getModernColor() + punishment.getStatus().getTitle());
            // Status-specific lore
            if (punishment.getStatus().equals(PunishmentStatus.REVOKED)) {
                // TODO: Find a better way to get the revoker's name
                PlayerAccount revoker = BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccount(punishment.getRevokerUuid()).join();
                lore.add("");
                lore.add("<l-gold>Revogada em: <l-gray>" + punishment.getRevokedAtFormatted());
                lore.add("<l-gold>Autor: <l-gray>" + revoker.playerName());
                lore.add("<l-gold>Motivo: <l-gray>" + punishment.getRevokeReason());
            }

            msg.hover(" <l-gray>• " + punishment.getStatus().getModernColor() + info.title(), lore.toArray(new String[0]));

            if (!punishment.getEvidence().startsWith("Não")) {
                msg.linkHover(" <l-white>[Prova]", punishment.getEvidence(), "<l-gray>Clique para copiar o link da prova.");
            } else {
                msg.simple(" <l-white>[Sem prova]");
            }

            if (punishment.getRevokerUuid() == null) {
                msg.actionHover(" <l-white>[Revogar]", ClickEvent.suggestCommand("/revogar " + punishment.getId()), "<l-gray>Clique para revogar esta punição.");
            }
            msg.newLine();
        }
        msg.newLine();
        msg.simple("<l-yellow>Legenda: ");
        msg.hover("<l-green>⬛ Ativa ", "<l-gray>O jogador está cumprindo o tempo da punição ainda, portanto, ela está ativa.");
        msg.hover("<l-yellow>⬛ Pendente ", "<l-gray>O jogador ainda não entrou no servidor após a aplicação da punição, portanto, ela está pendente.");
        msg.hover("<l-red>⬛ Finalizada ", "<l-gray>O jogador já cumpriu o tempo da punição, portanto, ela já foi finalizada.");
        msg.hover("<l-gray>⬛ Revogada ", "<l-gray>A punição foi revogada por algum motivo.");
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

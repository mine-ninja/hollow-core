package net.warcane.lugin.core.minecraft.punish.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.punish.data.*;
import net.warcane.lugin.core.minecraft.util.Cooldown;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import net.warcane.lugin.core.util.Tuple;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
public class PunishCommand extends SimpleCommand {

    public PunishCommand() {
        super("punir");
        setRequiredPermission("lugin.helper");
        setAliases(List.of("ban", "mute", "warn"));
        this.playersOnly = true;
    }

    @Override
    public void performCommand(CommandContext ctx) throws CommandFailedException {
        var onlyOneArg = ctx.isArgsLength(1);
        if (!onlyOneArg && !ctx.isArgsLength(2) && !ctx.isArgsLength(3)) {
            throw new CommandFailedException("§cUso correto: /punir <player> [id-motivo]");
        }

        var player = ctx.getSenderAsPlayer();

        player.sendMessage("§7Procurando...");

        var target = ctx.getRawArgOrNull(0);
        if (target == null) {
            StringUtils.send(player, "<l-error>Ocorreu um erro...");
            return;
        }

        if (onlyOneArg) {
            handleDisplayOptions(player, target);
            return;
        }

        int id = ctx.getIntOrDefault(1, -1);
        if (!checkPunishRequest(player, id, ctx)) {
            return;
        }

        BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccountByName(target).whenComplete((playerAccount, throwable) -> {
            Tasks.runSync(() -> {
                if (throwable != null) {
                    StringUtils.send(player, "<l-error>Ocorreu um erro ao buscar o jogador: " + throwable.getMessage());
                    return;
                }
                if (playerAccount == null) {
                    StringUtils.send(player, "<l-error>Jogador não encontrado.");
                    return;
                }

                if (Cooldown.isInCooldown(playerAccount.uniqueId(), "punished-" + id)) {
                    StringUtils.send(player, "<l-error>Este jogador já foi punido por este motivo recentemente. Aguarde um tempo antes de puni-lo novamente.");
                    return;
                }

                PunishManager.get().punishPlayer(playerAccount, player, PunishmentInfo.getPunishmentById(id), ctx.getRawArgOrNull(2));
                Cooldown.setCooldownSec(playerAccount.uniqueId(), 60 * 5L, "punished-" + id); // 5 minutes
            });

        });
    }

    private boolean checkPunishRequest(Player player, int id, CommandContext ctx) {
        if (id == -1) {
            StringUtils.send(player, "<l-error>ID inválido.");
            return false;
        }
        PunishmentInfo punishment = PunishmentInfo.getPunishmentById(id);
        if (punishment == null) {
            StringUtils.send(player, "<l-error>Punição não encontrada.");
            return false;
        }
        if (!player.hasPermission(punishment.mustHavePermission())) {
            StringUtils.send(player, "<l-error>Você não tem permissão para punir por este motivo.");
            return false;
        }
        boolean isNotGerente = !player.hasPermission("lugin.gerente");
        if (isNotGerente && !ctx.isArgsLength(3)) {
            StringUtils.send(player, "<l-error>É neccessário anexar uma prova para aplicar a punição.");
            return false;
        }
        try {
            String link = ctx.getRawArgOrNull(2);
            if (isNotGerente && !PunishManager.checkLink(link)) {
                StringUtils.send(player, "<l-error>O link inserido é inválido.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void handleDisplayOptions(Player player, String target) {
        BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccountByName(target).whenComplete((playerAccount, throwable) -> {
            var audience = BukkitPlatform.getInstance().getAdventure().player(player);
            ComponentBuilder builder = new ComponentBuilder()
                .newLine()
                .simple("<l-info>Punindo: " + target)
                .newLine()
                .simple("<l-info><l-yellow>Selecione um motivo:")
                .newLine()
                .newLine();
            for (PunishmentInfo punishment : PunishmentInfo.PUNISHMENTS) {
                if (!player.hasPermission(punishment.mustHavePermission())) continue;

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("<l-white>" + punishment.description());
                lore.add("");
                int banCount = 1;
                for (Tuple<PunishTime, PunishmentType> punishmentData : punishment.punishments()) {
                    lore.add("<l-yellow>" + banCount++ + "º: <l-white>" + punishmentData.b().getTitle() + " <l-gray>(" + punishmentData.a().getTitle() + ")");
                }
                lore.add("");
                lore.add("<l-white>Grupo mínimo: <l-green>" + MessageUtils.getFormatedPermission(punishment.mustHavePermission()));

                builder.simple(" <l-gray>• ")
                    .suggestHover("<l-white>" + punishment.title(), "/punir " + target + " " + punishment.id() + " ", lore.toArray(new String[0]));
                builder.newLine();
            }
            builder.newLine();
            builder.actionHover("  <l-red><b>CANCELAR", (audience1 -> {
                StringUtils.send(player, "\n\n\n<l-info>Ação cancelada com sucesso!\n");
            }), "<l-gray>Clique para cancelar a punição.");
            builder.send(audience);
        });
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
                if (punishment.title().toLowerCase().startsWith(prefix)) {
                    completions.add(String.valueOf(punishment.id()));
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

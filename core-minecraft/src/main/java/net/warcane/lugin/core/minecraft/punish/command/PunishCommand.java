package net.warcane.lugin.core.minecraft.punish.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.data.PunishmentInfo;
import net.warcane.lugin.core.minecraft.punish.utils.MessageUtils;
import net.warcane.lugin.core.minecraft.util.Cooldown;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
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
            player.sendMessage("§cOcorreu um erro...");
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
            if (throwable != null) {
                player.sendMessage("§cOcorreu um erro ao buscar o jogador: " + throwable.getMessage());
                return;
            }

            if (playerAccount == null) {
                player.sendMessage("§cJogador não encontrado.");
                return;
            }

            if (Cooldown.isInCooldown(playerAccount.uniqueId(), "punished-" + id)) {
                player.sendMessage("§cEste jogador já foi punido por este motivo recentemente. Aguarde um tempo antes de puni-lo novamente.");
                return;
            }

            PunishManager.get().punishPlayer(playerAccount, player, PunishmentInfo.getPunishmentById(id), ctx.getRawArgOrNull(2));
            Cooldown.setCooldownSec(playerAccount.uniqueId(), 60 * 5L, "punished-" + id); // 5 minutes
        });
    }

    private boolean checkPunishRequest(Player player, int id, CommandContext ctx) {
        if (id == -1) {
            player.sendMessage("§cID inválido.");
            return false;
        }
        PunishmentInfo punishment = PunishmentInfo.getPunishmentById(id);
        if (punishment == null) {
            player.sendMessage("§cPunição não encontrada.");
            return false;
        }
        if (!player.hasPermission(punishment.mustHavePermission())) {
            player.sendMessage("§cVocê não tem permissão para punir por este motivo.");
            return false;
        }
        boolean isNotGerente = !player.hasPermission("lugin.gerente");
        if (isNotGerente && !ctx.isArgsLength(3)) {
            player.sendMessage("§cÉ neccessário anexar uma prova para aplicar a punição.");
            return false;
        }
        try {
            String link = ctx.getRawArgOrNull(2);
            if (isNotGerente && !PunishManager.checkLink(link)) {
                player.sendMessage("§cO link inserido é inválido.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void handleDisplayOptions(Player player, String target) {
        BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccountByName(target).whenComplete((playerAccount, throwable) -> {
            player.sendMessage("§ePunindo: §f" + target);
            player.sendMessage("§e§lSelecione um motivo:");
            player.sendMessage("");

            var audience = BukkitPlatform.getInstance().getAdventure().player(player);

            for (PunishmentInfo punishment : PunishmentInfo.PUNISHMENTS) {
                if (punishment == null || !player.hasPermission(punishment.mustHavePermission())) {
                    continue;
                }

                var lore = "\n§f" + punishment.description() + "\n\n" +
                    punishment.punishments().stream()
                        .map(p -> "§e" + (punishment.punishments().indexOf(p) + 1) + "º: §f" +
                            p.b().getTitle() + " §7(" + p.a().getTitle() + ")")
                        .collect(Collectors.joining("\n")) +
                    "\n\n§fGrupo mínimo: §a" + MessageUtils.getFormatedPermission(punishment.mustHavePermission());

                var motivo = Component.text("§7• §f" + punishment.title())
                    .clickEvent(ClickEvent.suggestCommand("/punir " + target + " " + punishment.id() + " "))
                    .hoverEvent(HoverEvent.showText(Component.text(lore)));

                audience.sendMessage(motivo);
            }

            var cancelar = Component.text("§c§lCANCELAR: Clique aqui para cancelar")
                .clickEvent(ClickEvent.callback(audience1 -> {
                    player.sendMessage("\n\n\n§aAção cancelada com sucesso!\n");
                }))
                .hoverEvent(HoverEvent.showText(Component.text("§7Clique para cancelar a punição.")));
            audience.sendMessage(cancelar);
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

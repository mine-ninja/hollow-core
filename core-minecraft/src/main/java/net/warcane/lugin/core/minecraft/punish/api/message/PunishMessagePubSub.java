package net.warcane.lugin.core.minecraft.punish.api.message;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.MessageManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.PubSubMessage;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.punish.data.*;
import net.warcane.lugin.core.util.Tuple;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 * @project punish
 */
public record PunishMessagePubSub(UUID userKicked, String playerNick,
                                  PunishedDTO.Punishment punishment) implements PubSubMessage {

    public PunishMessagePubSub(String serialized) {
        this(
            UUID.fromString(serialized.split("-\\|")[0]),
            serialized.split("-\\|")[1],
            PunishedDTO.Punishment.fromRedis(serialized.split("-\\|")[2])
        );
    }

    @Override
    public String serialize() {
        return userKicked.toString() + "-|" + playerNick + "-|" + punishment.serializeToRedis();
    }

    @Override
    public String getChannel() {
        return MessageManager.PUNISH_MESSAGE_CHANNEL;
    }

    @Override
    public void handle() {
        var punishmentInfo = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());
        var punishmentType = punishmentInfo.getPunishment(punishment().getRepeatCount());

        if (!punishmentType.b().equals(PunishmentType.MUTE) && punishment.getStatus().equals(PunishmentStatus.ACTIVE)) {
            handleToPlayer(punishmentType, punishmentInfo.title());
        }

        var permission = "lugin.punish-message";
        var list = new ArrayList<String>();
        list.add(" \n");

        if (punishment.getStatus().equals(PunishmentStatus.REVOKED)) {
            list.add(" §e* A punição de " + playerNick + " foi revogada.");
            list.add("\n §e* Motivo: " + punishment.getRevokeReason());
            list.add("\n ");
            Bukkit.getOnlinePlayers().stream()
                .filter((player) -> player.hasPermission(permission))
                .forEach(p -> {
                    for (String msg : list) {
                        p.sendMessage(msg);
                    }
                });
            return;
        }

        if (punishment.getStatus().equals(PunishmentStatus.EXPIRED)) {
            list.add(" §e* A punição de " + playerNick + " expirou.");
            list.add("\n ");
            Bukkit.getOnlinePlayers().stream()
                .filter((player) -> player.hasPermission(permission))
                .forEach(p -> {
                    for (String msg : list) {
                        p.sendMessage(msg);
                    }
                });
            return;
        }

        list.add(" §c* " + playerNick + " foi "
                 + (punishmentType.b().equals(PunishmentType.MUTE) ? "mutado" : "banido") + ".");
        list.add(" §c* Motivo: " + punishmentInfo.title() + " - " + punishment.getEvidence());
        list.add(" §c* Duração: " + punishmentType.a().getTitle());
        list.add(" ");

        Bukkit.getOnlinePlayers().stream()
            .filter((player -> player.hasPermission(permission) && !player.getUniqueId().equals(userKicked)))
            .forEach(p -> {
                for (String msg : list) {
                    p.sendMessage(msg);
                }
            });
    }

    private void handleToPlayer(Tuple<PunishTime, PunishmentType> punishmentType, String reason) {
        var player = Bukkit.getPlayer(userKicked);
        if (player == null || !player.isOnline()) {
            return;
        }
        PunishManager.get().loadPlayer(player);

        ComponentBuilder sb = new ComponentBuilder();
        sb.simple("<l-blue><bold>LUGIN</bold></l-blue>").newLine();
        sb.newLine();
        sb.simple("<l-red>Você foi banido!").newLine();
        if (punishmentType.b().equals(PunishmentType.PERM)) {
            sb.simple("<l-red>Sua punição é permanente.").newLine();
        } else {
            sb.simple("<l-red>Sua punição expira em " + punishmentType.a().getTitle() + ".").newLine();
        }
        sb.newLine();
        sb.simple("<l-red>Motivo: " + reason).newLine();
        sb.simple("<l-red>Prova: <u>" + punishment.getEvidence() + "</u>.").newLine();
        sb.newLine();
        sb.simple("<l-red>Caso ache que a sua punição foi aplicada de maneira incorreta,").newLine();
        sb.simple("<l-red>faça uma revisão acessando <l-yellow><u>discord.gg/lugin</u> <l-red>com o ID <l-yellow>#" + punishment.getId() + "<l-red>.");

        // Its not possible to kick a player from async :)
        Tasks.runSync(() -> {
            player.kick(sb.build());
        });
    }
}

package net.warcane.lugin.core.minecraft.punish.api.message;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.MessageManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.PubSubMessage;
import net.warcane.lugin.core.minecraft.punish.data.*;
import net.warcane.lugin.core.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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
        PunishmentInfo punishmentInfo = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());
        Tuple<PunishTime, PunishmentType> punishmentType = punishmentInfo.getPunishment(punishment().getRepeatCount());

        if (!punishmentType.b().equals(PunishmentType.MUTE) && punishment.getStatus().equals(PunishmentStatus.ACTIVE)) {
            handleToPlayer(punishmentType, punishmentInfo.title());
        }
        BukkitPlatform.getInstance().getPlayerAccountService().getPlayerAccount(punishment.getPunisherUuid()).whenComplete((punisherAccount, e) -> {

            if (e != null) {
                Bukkit.getLogger().severe("Erro ao buscar conta do punidor: " + e.getMessage());
                return;
            }
            String staffer = punisherAccount.playerName();
            String permission = "lugin.punish-message";
            List<String> list = new ArrayList<>();
            list.add(" \n");
            if (punishment.getStatus().equals(PunishmentStatus.REVOKED)) {
                list.add(" §e* " + staffer + " revogou a punição de " + playerNick + ".");
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
        });
    }

    private void handleToPlayer(Tuple<PunishTime, PunishmentType> punishmentType, String reason) {
        Player player = Bukkit.getPlayer(userKicked);
        if (player == null) return;
        if (!player.isOnline()) return;

        StringBuilder sb = new StringBuilder("§b§lLUGIN\n");
        sb.append("\n");
        sb.append("§cVocê foi banido!\n");
        if (punishmentType.b().equals(PunishmentType.PERM)) {
            sb.append("§cSua punição é permanente.\n");
        } else {
            sb.append("§cSua punição expira em ").append(punishmentType.a().getTitle()).append(".\n");
        }
        sb.append("\n");
        sb.append("§cMotivo: ").append(reason).append("\n");
        sb.append("§cProva: §n").append(punishment.getEvidence()).append("§r\n");
        sb.append("\n");
        sb.append("§cCaso ache que a sua punição foi aplicada de maneira incorreta,\n");
        sb.append("§cfaça uma revisão acessando §ediscord.gg/lugin §ccom o ID §e#").append(punishment.getId()).append("§c.\n");

        // Its not possible to kick a player from async :)
        Bukkit.getScheduler().runTask(BukkitPlatformPlugin.getInstance(), () -> {
            player.kickPlayer(sb.toString());
        });
    }
}

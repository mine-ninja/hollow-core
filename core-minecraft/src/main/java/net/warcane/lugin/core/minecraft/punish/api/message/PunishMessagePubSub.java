package net.warcane.lugin.core.minecraft.punish.api.message;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.MessageManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.PubSubMessage;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.punish.data.*;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import net.warcane.lugin.core.util.Tuple;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
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

        StringBuilder sb = new StringBuilder("§b§lLUGIN\n");
        sb.append("\n");
        sb.append("§cVocê foi banido!\n");
        if (punishmentType.b().equals(PunishmentType.PERM)) {
            sb.append("§cSua punição é permanente.\n");
        } else {
            sb.append("§cSua punição expira em ").append(MessageUtils.getFormattedTime(punishment.getExpiresAt())).append(".\n");
        }
        sb.append("\n");
        sb.append("§cMotivo: ").append(reason).append("\n");
        sb.append("§cProva: §n").append(punishment.getEvidence()).append("§r\n");
        sb.append("\n");
        sb.append("§cCaso ache que a sua punição foi aplicada de maneira incorreta,\n");
        sb.append("§cfaça uma revisão acessando §ediscord.gg/lugin §ccom o ID §e#").append(punishment.getId()).append("§c.\n");

        // Its not possible to kick a player from async :)
        Tasks.runSync(() -> {
            var version = PacketEvents.getAPI().getServerManager().getVersion();

            if (version.isOlderThan(ServerVersion.V_1_9)) {
                player.kickPlayer(sb.toString());
                return;
            }

            player.kick(StringUtils.formatString(sb.toString()));
        });
    }
}

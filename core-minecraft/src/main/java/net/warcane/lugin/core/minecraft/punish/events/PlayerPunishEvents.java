package net.warcane.lugin.core.minecraft.punish.events;

import io.papermc.paper.event.player.ChatEvent;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.data.*;
import net.warcane.lugin.core.minecraft.punish.utils.MessageUtils;
import net.warcane.lugin.core.minecraft.util.Tuple;
import net.warcane.lugin.core.minecraft.util.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 12/07/2025
 * @project punish
 */
public class PlayerPunishEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        PunishedDTO punishedDTO = PunishManager.get().getPunishedPlayer(uuid).join();
        if (punishedDTO == null) return;
        Tuple<PunishTime, PunishmentType> highestPunishment = null;
        PunishedDTO.Punishment punishmentFinal = null;
        String motive = "";
        for (PunishedDTO.Punishment punishment : punishedDTO.getPunishments()) {
            if (punishment.getStatus() != PunishmentStatus.ACTIVE) continue;
            long timeToExpire = punishment.getExpiresAt() - System.currentTimeMillis();

            PunishmentInfo punishmentInfo = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());
            Tuple<PunishTime, PunishmentType> punishInfo = punishmentInfo.getPunishment(punishment.getRepeatCount());

            if (punishInfo.b() == PunishmentType.MUTE) continue;

            if (punishInfo.a().getTimeInMilliseconds() == -1) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
                break;
            }
            if (highestPunishment == null) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
                continue;
            }
            if (timeToExpire <= 0 ) {
                punishment.setStatus(PunishmentStatus.EXPIRED);
                PunishManager.get().updatePunishmentStatus(punishment.getId(), punishment);
                continue;
            }
            if (highestPunishment.a().getTimeInMilliseconds() < punishInfo.a().getTimeInMilliseconds()) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
            }
        }
        if (highestPunishment == null) return;

        String message = getBannedPlayerMessage(highestPunishment, motive, punishmentFinal);
        event.kickMessage(StringUtils.text(message));
        event.setResult(PlayerPreLoginEvent.Result.KICK_BANNED);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PunishManager.get().loadPlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PunishManager.get().unloadPlayer(player);
    }

    /*@EventHandler
    public void onPlayerTell(TellEvent event) {
        Player player = event.getPlayer();
        PunishedDTO dto = PunishManager.get().getCachedPlayer(player.getUniqueId());
        muteReply(player, dto, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatGlobal(ChatLocalEvent event) {
        Player player = event.getPlayer();
        PunishedDTO dto = PunishManager.get().getCachedPlayer(player.getUniqueId());
        muteReply(player, dto, event);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatLocal(ChatGlobalEvent event) {
        Player player = event.getPlayer();
        PunishedDTO dto = PunishManager.get().getCachedPlayer(player.getUniqueId());
        muteReply(player, dto, event);
    }*/

    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        PunishedDTO dto = PunishManager.get().getCachedPlayer(event.getPlayer().getUniqueId());
        PlayerPunishEvents.muteReply(event.getPlayer(), dto, event);
    }


    public static void muteReply(Player player, PunishedDTO dto, Cancellable event) {
        if (dto != null) {
            for (PunishedDTO.Punishment punishment : dto.getPunishments()) {
                long timeToExpire = punishment.getExpiresAt() - System.currentTimeMillis();
                if (timeToExpire <= 0 && punishment.getStatus() == PunishmentStatus.ACTIVE) {
                    punishment.setStatus(PunishmentStatus.EXPIRED);
                    PunishManager.get().updatePunishmentStatus(punishment.getId(), punishment);
                    continue;
                }

                if (punishment.getStatus() == PunishmentStatus.ACTIVE) {
                    PunishmentInfo punishmentById = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());

                    int count = punishment.getRepeatCount();

                    if (punishmentById.getPunishment(count).b() == PunishmentType.MUTE) {
                        event.setCancelled(true);

                        String time = MessageUtils.formatMilliseconds(timeToExpire);

                        player.sendMessage(" ");
                        player.sendMessage("§cVocê está silenciado. Sua punição irá expirar em " + time);
                        player.sendMessage(" ");
                        player.sendMessage("§c * Motivo: " + punishmentById.title());
                        player.sendMessage("§c * Prova: " + punishment.getEvidence());
                        player.sendMessage("§c * ID: #" + punishment.getId());
                        player.sendMessage(" ");
                        player.sendMessage("§cCaso ache que a sua punição tenha sido aplicada de maneira incorreta acesse §ediscord.gg/lugin§c para criar uma revisão.");
                        player.sendMessage(" ");
                    }
                }
            }
        } else {
            event.setCancelled(true);
            player.sendMessage("§cSua conta não foi carregada completamente. Espere alguns segundos.");
        }
    }


    private String getBannedPlayerMessage(Tuple<PunishTime, PunishmentType> punishmentType, String motive, PunishedDTO.Punishment punishment) {
        StringBuilder sb = new StringBuilder("<l-blue><bold>LUGIN</bold>\n");
        sb.append("\n");
        sb.append("<l-red>Você foi banido!\n");
        if (punishmentType.b().equals(PunishmentType.PERM)) {
            sb.append("<l-red>Sua punição é permanente.\n");
        } else {
            sb.append("<l-red>Sua punição expira em ").append(punishmentType.a().getTitle()).append(".\n");
        }
        sb.append("\n");
        sb.append("<l-red>Motivo: ").append(motive).append("\n");
        sb.append("<l-red>Prova: <u>").append(punishment.getEvidence()).append("</u><reset>\n");
        sb.append("\n");
        sb.append("<l-red>Caso ache que a sua punição foi aplicada de maneira incorreta,\n");
        sb.append("<l-red>faça uma revisão acessando <l-yellow>discord.gg/lugin <l-red>com o ID <l-yellow>#").append(punishment.getId()).append("<l-red>.\n");
        return sb.toString();
    }
}

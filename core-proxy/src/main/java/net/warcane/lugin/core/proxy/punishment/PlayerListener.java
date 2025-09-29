package net.warcane.lugin.core.proxy.punishment;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.punish.api.PunishManager;
import net.warcane.lugin.core.punish.data.*;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import net.warcane.lugin.core.util.Tuple;

public class PlayerListener {

    @Subscribe
    public void onJoin(LoginEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        var punishedDTO = PunishManager.get().getPunishedPlayer(uuid).join();

        if (punishedDTO == null) {
            return;
        }

        Tuple<PunishTime, PunishmentType> highestPunishment = null;
        PunishedDTO.Punishment punishmentFinal = null;

        var motive = "";

        for (var punishment : punishedDTO.getPunishments()) {
            if (punishment.getStatus() != PunishmentStatus.ACTIVE) {
                continue;
            }

            var punishmentInfo = PunishmentInfo.getPunishmentById(punishment.getPunishmentInfoId());
            var punishInfo = punishmentInfo.getPunishment(punishment.getRepeatCount());

            if (punishInfo.b() == PunishmentType.MUTE) {
                continue;
            }

            if (punishInfo.b() == PunishmentType.PERM) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
                break;
            }

            if (highestPunishment == null || punishInfo.a().getTimeInMilliseconds() == -1) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
                if (punishInfo.a().getTimeInMilliseconds() == -1) {
                    break;
                }
                continue;
            }

            if (punishment.getExpiresAt() >= System.currentTimeMillis() &&
                highestPunishment.a().getTimeInMilliseconds() < punishInfo.a().getTimeInMilliseconds()) {
                highestPunishment = punishInfo;
                punishmentFinal = punishment;
                motive = punishmentInfo.title();
            }
        }

        if (highestPunishment == null) {
            return;
        }

        var message = MessageUtils.getBannedPlayerMessage(highestPunishment, motive, punishmentFinal);
        event.setResult(ResultedEvent.ComponentResult.denied(Component.text(message)));
    }
}

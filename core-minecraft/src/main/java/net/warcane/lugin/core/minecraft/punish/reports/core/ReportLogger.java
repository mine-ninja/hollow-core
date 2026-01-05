package net.warcane.lugin.core.minecraft.punish.reports.core;

import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.DiscordWebhook;
import net.warcane.lugin.core.minecraft.util.Logger;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentInfo;

import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 10/09/2025
 */
public class ReportLogger extends Logger {

    public ReportLogger(File dataFolder) {
        super(dataFolder,
            "https://discord.com/api/webhooks/1415314834953601095/f830Vo2mR4AU8v0MPgRhxlZCGfiRRYm1WOT8KJCOkds7e3tLnfn8ANXoBaLVF4cxGGIA",
            "reports-");
    }

    public void logReport(String reporterName, PunishmentInfo info, String targetName, UUID targetUuid, String evidenceLink) {
        Tasks.runAsync(() -> {
            String logEntry = reportToLog(reporterName, info, targetName, targetUuid, evidenceLink);
            DiscordWebhook.EmbedObject embed = reportToEmbed(reporterName, info, targetName, targetUuid, evidenceLink);
            writeFile(logEntry);
            sendEmbed(embed);
        });
    }

    private String reportToLog(String reporterName, PunishmentInfo info, String name, UUID uuid, String evidenceLink) {
        return String.format("Reportado: %s (%s) | Tipo: %s | Evidência: %s | Reportado por: %s",
            name, uuid, info.title(), evidenceLink, reporterName);
    }

    private DiscordWebhook.EmbedObject reportToEmbed(String reporterName, PunishmentInfo info, String name, UUID uuid, String evidenceLink) {
        var embed = new DiscordWebhook.EmbedObject();

        embed.setTitle("Reportado: " + name + " (" + uuid + ")");
        embed.setColor(Color.YELLOW);

        embed.addField("Reportado por:", reporterName, true);
        embed.addField("Tipo:", info.title(), false);
        embed.addField("Evidência:", evidenceLink, false);

        embed.setFooter("Data: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), "");

        return embed;
    }
}

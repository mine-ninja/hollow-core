package net.warcane.lugin.core.minecraft.punish.core;

import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.DiscordWebhook;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentInfo;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 10/09/2025
 */
public class PunishLogger {
    private final File logFile;

    private final LocalDateTime localDateTime;

    public PunishLogger(File dataFolder) {
        if (!dataFolder.exists() && !dataFolder.mkdir())
            throw new IllegalStateException("Failed to create data folder: " + dataFolder.getAbsolutePath());
        this.localDateTime = LocalDateTime.now();
        this.logFile = new File(dataFolder, this.localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log");
        if (this.logFile.exists()) return;
        try {
            this.logFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logRevoke(PunishedDTO.Punishment punishment, String revokerName) {
        Tasks.runAsync(() -> {
            String logEntry = revokeToLog(punishment, revokerName);
            DiscordWebhook.EmbedObject embed = revokeToEmbed(punishment, revokerName);
            writeFile(logEntry);
            sendEmbed(embed);
        });
    }

    public void logPunish(PunishedDTO.Punishment punishment, String punisherName, PunishmentInfo info, String name, UUID uuid) {
        Tasks.runAsync(() -> {
            String logEntry = punishmentToLog(punishment, punisherName, info, name, uuid);
            DiscordWebhook.EmbedObject embed = punishmentToEmbed(punishment, punisherName, info, name, uuid);
            writeFile(logEntry);
            sendEmbed(embed);
        });
    }

    private void writeFile(String logEntry) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(this.logFile, true));
            try {
                writer.write(logEntry);
                writer.newLine();
                writer.close();
            } catch (Throwable throwable) {
                try {
                    writer.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendEmbed(DiscordWebhook.EmbedObject embed) {
        try {
            var discordWebhook = new DiscordWebhook("https://discord.com/api/webhooks/1415314834953601095/f830Vo2mR4AU8v0MPgRhxlZCGfiRRYm1WOT8KJCOkds7e3tLnfn8ANXoBaLVF4cxGGIA");
            discordWebhook.addEmbed(embed);
            discordWebhook.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String punishmentToLog(PunishedDTO.Punishment punishment, String punisherName, PunishmentInfo info, String name, UUID uuid) {
        return String.format("Punindo: %s (%s) | Tipo: %s (x%d) | Evidência: %s | Punido por: %s | Duração: %s | ID: %d",
            name, uuid, info.title(), punishment.getRepeatCount(), punishment.getEvidence(), punisherName,
            punishment.getExpiresAtFormatted(), punishment.getId());
    }

    private DiscordWebhook.EmbedObject punishmentToEmbed(PunishedDTO.Punishment punishment, String punisherName, PunishmentInfo info, String name, UUID uuid) {
        var embed = new DiscordWebhook.EmbedObject();

        embed.setTitle("Punindo: " + name + " (" + uuid + ")");
        embed.setColor(Color.RED);

        embed.addField("Tipo de Punimento:", info.title() + " (x" + (punishment.getRepeatCount() + 1) + ")", true);
        embed.addField("Evidência:", punishment.getEvidence(), false);
        embed.addField("Punido por:", punisherName, false);
        embed.addField("Duração:", punishment.getExpiresAtFormatted(), false);
        embed.addField("ID do Punimento:", String.valueOf(punishment.getId()), false);

        embed.setFooter("Data: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), "");

        return embed;
    }

    private String revokeToLog(PunishedDTO.Punishment punishment, String revokerName) {
        return String.format("Revoke ID: %d | Evidência: %s | Revogado por: %s | Revogado em: %s | Motivo: %s",
            punishment.getId(), punishment.getEvidence(), revokerName, punishment.getRevokedAtFormatted(),
            punishment.getRevokeReason() != null ? punishment.getRevokeReason() : "Nenhum motivo fornecido");
    }

    private DiscordWebhook.EmbedObject revokeToEmbed(PunishedDTO.Punishment punishment, String revokerName) {
        var embed = new DiscordWebhook.EmbedObject();

        embed.setTitle("Revogando punimento de ID: " + punishment.getId());
        embed.setColor(Color.YELLOW);

        embed.addField("Evidência:", punishment.getEvidence(), false);
        embed.addField("Revogado por:", revokerName, false);
        embed.addField("Revogado em:", punishment.getRevokedAtFormatted(), false);
        embed.addField("Motivo da Revogação:", punishment.getRevokeReason() != null ? punishment.getRevokeReason() : "Nenhum motivo fornecido", false);

        embed.setFooter("Data: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), "");

        return embed;
    }
}

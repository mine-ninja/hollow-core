package io.github.minehollow.minecraft.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
public abstract class Logger {
    private final String webhookUrl;

    protected final File logFile;

    protected final LocalDateTime localDateTime;

    public Logger(File dataFolder, String webhookUrl) {
        this(dataFolder, webhookUrl, "");
    }

    public Logger(File dataFolder, String webhookUrl, String filePrefix) {
        this.webhookUrl = webhookUrl;
        if (!dataFolder.exists() && !dataFolder.mkdir())
            throw new IllegalStateException("Failed to create data folder: " + dataFolder.getAbsolutePath());
        this.localDateTime = LocalDateTime.now();
        this.logFile = new File(dataFolder, filePrefix + this.localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log");
        if (this.logFile.exists()) return;
        try {
            this.logFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void writeFile(String logEntry) {
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



    protected void sendEmbed(DiscordWebhook.EmbedObject embed) {
        try {
            var discordWebhook = new DiscordWebhook(webhookUrl);
            discordWebhook.addEmbed(embed);
            discordWebhook.execute();
        } catch (IOException e) {
            log.error("Failed to send log to Discord webhook: {}\n{}", e.getMessage(), e.getStackTrace());
        }
    }
}

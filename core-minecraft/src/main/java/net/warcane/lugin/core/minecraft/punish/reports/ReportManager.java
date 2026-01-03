package net.warcane.lugin.core.minecraft.punish.reports;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.warcane.lugin.core.database.MongoCounterService;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.api.message.PunishMessagePubSub;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.MessageManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.RedisDatabase;
import net.warcane.lugin.core.minecraft.punish.events.PlayerPunishEvents;
import net.warcane.lugin.core.minecraft.punish.reports.core.ReportLogger;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentInfo;
import net.warcane.lugin.core.punish.data.PunishmentStatus;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;


/**
 * @author Rok, Pedro Lucas nmm. Created on 01/07/2025
 */
public class ReportManager {

    private static ReportManager instance;

    private final ReportLogger reportLogger;

    public ReportManager(Plugin plugin) {
        this.reportLogger = new ReportLogger(plugin.getDataFolder());
    }

    public void reportPlayer(PlayerAccount reportedPlayer, Player reporter, PunishmentInfo reportReason, String evidenceLink) {
        var audience = BukkitPlatformPlugin.getInstance().adventure().player(reporter);
        reportLogger.logReport(reporter.getName(), reportReason, reportedPlayer.playerName(), reportedPlayer.uniqueId(), evidenceLink);
        StringUtils.send(audience, "<l-confirm>Você reportou <l-yellow>" + reportedPlayer.playerName() + " <l-green> com sucesso!");
    }

    public static void init(Plugin plugin) {
        if (instance != null) throw new IllegalStateException("ReportManager already initialized!");
        instance = new ReportManager(plugin);
    }

    public static ReportManager get() {
        if (instance == null) throw new IllegalStateException("ReportManager not initialized!");
        return instance;
    }
}

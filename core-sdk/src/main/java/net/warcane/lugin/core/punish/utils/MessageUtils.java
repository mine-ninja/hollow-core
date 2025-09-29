package net.warcane.lugin.core.punish.utils;

import net.warcane.lugin.core.punish.data.PunishTime;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentType;
import net.warcane.lugin.core.util.Tuple;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 */
public class MessageUtils {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static String getFormattedTime(long timeInMillis) {
        var dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        return formatDate(Instant.ofEpochMilli(timeInMillis), dateFormat);
    }

    public static String getFormattedTimeSimple(long timeInMillis) {
        var dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatDate(Instant.ofEpochMilli(timeInMillis), dateFormat);
    }

    public static String formatDate(Instant instant, DateTimeFormatter formatter) {
        return formatter.withZone(ZONE).format(instant);
    }

    public static String formatMilliseconds(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 30;
        months %= 12;

        StringBuilder result = new StringBuilder();

        if (years > 0) result.append(years).append("a ");
        if (months > 0) result.append(months).append("me ");
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0 || result.isEmpty()) result.append(seconds).append("s");

        return result.toString().trim();
    }

    public static String getBannedPlayerMessage(Tuple<PunishTime, PunishmentType> punishmentType, String motive, PunishedDTO.Punishment punishment) {
        var punishmentMessage = punishmentType.b().equals(PunishmentType.PERM)
            ? "§cSua punição é permanente.\n"
            : "§cSua punição expira em " + punishmentType.a().getTitle() + ".\n";

        return "§b§lLUGIN\n\n" +
            "§cVocê foi banido!\n" +
            punishmentMessage +
            "\n§cMotivo: " + motive + "\n" +
            "§cProva: §n" + punishment.getEvidence() + "§r\n\n" +
            "§cCaso ache que a sua punição foi aplicada de maneira incorreta,\n" +
            "§cfaça uma revisão acessando §ediscord.gg/lugin §ccom o ID §e#" +
            punishment.getId() + "§c.\n";
    }

    public static String getFormattedPermission(String permission) {
        return switch (permission) {
            case "lugin.helper" -> "<l-green>Ajudante";
            case "lugin.moderator" -> "<l-green>Moderador";
            case "lugin.admin" -> "<red>Admin";
            case "lugin.gerente" -> "<l-red>Gerente";
            default -> permission;
        };
    }
}

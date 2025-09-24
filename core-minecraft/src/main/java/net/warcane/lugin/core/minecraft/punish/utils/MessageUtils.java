package net.warcane.lugin.core.minecraft.punish.utils;

import java.text.SimpleDateFormat;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 * @project punish
 */
public class MessageUtils {

    public static String getFormattedTime(long timeInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");
        return dateFormat.format(timeInMillis);
    }

    public static String getFromattedTimeSimple(long timeInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(timeInMillis);
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


    public static String getFormatedPermission(String permission) {
        return switch (permission) {
            case "lugin.helper" -> "<l-green>Ajudante";
            case "lugin.moderator" -> "<l-blue>Moderador";
            case "lugin.admin" -> "<red>Admin";
            case "lugin.gerente" -> "<l-red>Gerente";
            default -> permission;
        };
    }
}

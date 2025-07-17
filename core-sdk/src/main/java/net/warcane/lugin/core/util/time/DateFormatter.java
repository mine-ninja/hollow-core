package net.warcane.lugin.core.util.time;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm");


    public static String format(long time) {
        return FORMATTER.format(time);
    }

    /**
     * Converte um Instant para uma String formatada de data e hora (dd/MM/yyyy hh:mm),
     * utilizando o fuso horário padrão do sistema.
     *
     * @param instant O objeto Instant a ser formatado.
     * @return Uma String contendo a data e hora formatadas, ou null se o Instant for null.
     */
    public static String format(@NotNull Instant instant) {
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        return zonedDateTime.format(DATE_TIME_FORMATTER);
    }
}

package net.warcane.lugin.core.util.time;

import java.text.SimpleDateFormat;

public class DateFormatter {

    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    public static String format(long time) {
        return FORMATTER.format(time);
    }
}

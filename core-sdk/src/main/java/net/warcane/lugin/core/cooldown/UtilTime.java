package net.warcane.lugin.core.cooldown;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@UtilityClass
public final class UtilTime {

    private static final Pattern timePattern = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
    private static final Map<Character, Integer> timeUnit = new HashMap<Character, Integer>() {{
        put('a', 31557600);
        put('y', 31104000);
        put('M', 2592000);
        put('w', 604800);
        put('d', 86400);
        put('h', 3600);
        put('m', 60);
        put('s', 1);
    }};

    @Getter
    private static final ZoneId zoneId;
    private static final ZoneOffset zoneOffset;
    @Getter
    private static final DateTimeFormatter dateFormatter, dateFormatterDDMMUUUU, dateFormatterHHMMSS;

    static {
        Locale locale = new Locale("pt", "BR");
        Locale.setDefault(locale);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        zoneId = ZoneId.of("America/Sao_Paulo");
        zoneOffset = ZoneOffset.of("-03:00");

        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss")
          .withResolverStyle(ResolverStyle.STRICT)
          .withZone(zoneId);

        dateFormatterDDMMUUUU = DateTimeFormatter.ofPattern("dd/MM/uuuu")
          .withResolverStyle(ResolverStyle.STRICT)
          .withZone(zoneId);

        dateFormatterHHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss")
          .withResolverStyle(ResolverStyle.STRICT)
          .withZone(zoneId);
    }

    public static String formatMMSS(int i) {
        int m = i / 60;
        int s = i % 60;
        return m + ":" + (s > 9 ? s : "0" + s);
    }

    public static String formatMMSSMS(long ms) {
        double seconds = (double) ms / 1000.0D;
        return String.format(Locale.ENGLISH, "%02d:" +
                                             (seconds % 60.0D < 10 ? "0" : "") + "%02.3f", ms / 60000L % 60L, seconds % 60.0D);
    }

    public static String simpleFormatSSMS(long ms) {
        long m = ms / 100;
        long seconds = ms / 1000;
        return seconds + "." + m + "s";
    }

    public static String formatDDHHMMSS(int seconds) {
        return formatShortCounter(seconds, TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);
    }

    public static String formatDDHHMMSS(long seconds) {
        return formatShortCounter(seconds, TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);
    }

    public static String formatHHMMSS(int seconds) {
        return formatShortCounter(seconds, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);
    }

    public static String formatDDHHMM(long seconds) {
        return formatShortCounter(seconds, TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES);
    }

    public static String formatDDHH(int seconds) {
        return formatShortCounter(seconds, TimeUnit.DAYS, TimeUnit.HOURS);
    }

    public static String formatShortCounter(long seconds, TimeUnit... timeUnit) {
        return formatShortCounter(seconds, Set.of(timeUnit));
    }

    public static String formatShortCounter(long i, Collection<TimeUnit> timeUnit) {
        long days = 0, hours = 0, minutes = 0, seconds = 0;
        if (timeUnit.contains(TimeUnit.DAYS)) {
            days = i / 86400;
            i -= (days * 86400);
        }
        if (timeUnit.contains(TimeUnit.HOURS)) {
            hours += i / 3600;
            i -= (hours * 3600);
        }
        if (timeUnit.contains(TimeUnit.MINUTES)) {
            minutes += i / 60;
            i -= (minutes * 60);
        }

        seconds += i;

        boolean before = false;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d");
            before = true;
        }
        if (hours > 0) {
            builder.append(before ? " " : "").append(hours).append("h");
            before = true;
        }
        if (minutes > 0) {
            builder.append(before ? " " : "").append(minutes).append("m");
            before = true;
        }
        if (seconds > 0 && timeUnit.contains(TimeUnit.SECONDS)) {
            builder.append(before ? " " : "").append(seconds).append("s");
        }
        String s = builder.toString();
        if (s.isEmpty()) {
            return seconds + "s";
        }
        return s;
    }

    //Time left
    public static String formatLongCounter(long timeoutSeconds) {
        StringBuilder stringBuilder = new StringBuilder();

        int days = (int) (timeoutSeconds / 86400);
        int hours = (int) (timeoutSeconds / 3600) % 24;
        int minutes = (int) (timeoutSeconds / 60) % 60;
        int seconds = (int) timeoutSeconds % 60;
        boolean first = false;

        if (days > 0) {
            first = true;
            stringBuilder.append(days).append(" dia").append(days != 1 ? "s" : "");
        }
        if (hours > 0) {
            if (first)
                stringBuilder.append(" ");
            first = true;
            stringBuilder.append(hours).append(" hora").append(hours != 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (first)
                stringBuilder.append(" ");
            first = true;
            stringBuilder.append(minutes).append(" minuto").append(minutes != 1 ? "s" : "");
        }
        if (seconds > 0) {
            if (first)
                stringBuilder.append(" ");
            stringBuilder.append(seconds).append(" segundo").append(seconds != 1 ? "s" : "");
        }

        return stringBuilder.toString();
    }

    public static String formatLongCounter(Duration duration) {
        return formatLongCounter(duration.getSeconds());
    }

    public static String formatLongCounter(Instant instant) {
        return formatLongCounter(Duration.between(Instant.now(), instant));
    }

    public static String formatMediumCounter(Duration duration) {
        return formatMediumCounter(duration.getSeconds());
    }

    public static String formatMediumCounter(Instant instant) {
        return formatMediumCounter(Duration.between(Instant.now(), instant));
    }

    public static String formatMediumCounter(long timeoutSeconds) {
        int days = (int) (timeoutSeconds / 86400);
        if (days > 0) {
            return days + " dia" + (days != 1 ? "s" : "");
        }

        int hours = (int) (timeoutSeconds / 3600) % 24;
        if (hours > 0) {
            return hours + " hora" + (hours != 1 ? "s" : "");
        }

        int minutes = (int) (timeoutSeconds / 60) % 60;
        if (minutes > 0) {
            return minutes + " minuto" + (minutes != 1 ? "s" : "");
        }

        return "Menos de um minuto";
    }

    public static long toMillis(String time) {
        int seconds = 0;

        Integer i = null;
        Character ch;

        String[] array = timePattern.split(time);

        if (array.length % 2 != 0)
            return -1;

        for (String s : array) {
            if (i == null) {
                i = Integer.parseInt(s);
            } else {
                if (s.length() > 1) {
                    return -1;
                }
                ch = s.charAt(0);
                if (!timeUnit.containsKey(ch)) {
                    return -1;
                }
                seconds += (i * timeUnit.get(ch));
                i = null;
            }
        }
        return (seconds <= 0 ? -1 : seconds * 1000L);
    }

    public static String timeLeftDDHHMMSS(Instant instant) {
        return formatDDHHMMSS(Duration.between(Instant.now(), instant).getSeconds());
    }

    public static String timeLeftShort(Instant instant) {
        long seconds = Duration.between(Instant.now(), instant).getSeconds();
        if (seconds <= 60)
            return formatDDHHMMSS(seconds);
        return formatDDHHMM(seconds);
    }

    public static String mediumFormat(Duration duration) {
        return duration.toString()
          .substring(2)
          .replaceAll("(\\d[HMS])(?!$)", "$1 ")
          .toLowerCase();
    }

    public static long getDistanceMilli(Instant instant) {
        return instant.toEpochMilli() - Instant.now().toEpochMilli();
    }

    public static long getDistanceSeconds(Instant instant) {
        return getDistanceMilli(instant) / 1000;
    }

    public static Instant toInstant(Date date) {
        if (date == null)
            return null;
        return date.toInstant();
    }

    public static Instant toInstant(LocalDateTime date) {
        if (date == null)
            return null;
        return date.toInstant(zoneOffset);
    }

    public static Instant toInstant(LocalDate date) {
        if (date == null)
            return null;
        return date.atTime(0, 0).toInstant(zoneOffset);
    }

    public static Timestamp toTimestamp(Instant instant) {
        if (instant == null)
            return null;
        return Timestamp.from(instant);
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null)
            return null;
        return LocalDateTime.ofInstant(date.toInstant(), zoneId);
    }

    public static LocalDate toLocalDate(Instant date) {
        return date.atZone(zoneId).toLocalDate();
    }

    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null)
            return null;
        return Timestamp.valueOf(localDateTime);
    }

    public static String formatDateDDMMYYYY(long time) {
        return formatDateDDMMYYYY(Instant.ofEpochMilli(time));
    }

    public static String formatDateDDMMYYYY(Instant temporal) {
        return dateFormatterDDMMUUUU.format(temporal);
    }

    public static String formatDateDDMMYYYY(Temporal temporal) {
        return dateFormatterDDMMUUUU.format(temporal);
    }

    public static String formatDate(Date time) {
        return formatDate(time.toInstant());
    }

    public static String formatDate(long time) {
        return formatDate(Instant.ofEpochMilli(time));
    }

    public static String formatDate(Temporal temporal) {
        return dateFormatter.format(temporal);
    }

    public static Instant parseToInstant(String date) {
        return parseToInstant(date, dateFormatter);
    }

    public static Instant parseToInstant(String date, DateTimeFormatter dateFormatter) {
        LocalDateTime dateTime = LocalDateTime.parse(date, dateFormatter);
        return dateTime.toInstant(zoneOffset);
    }

    public static LocalDateTime parseToLocalDateTime(String date) {
        return LocalDateTime.parse(date, dateFormatter);
    }

    public static LocalDate parseToLocalDate(String date) {
        return LocalDate.parse(date, dateFormatter);
    }

    public static LocalTime parseToLocalTime(String date) {
        return LocalTime.parse(date, dateFormatter);
    }


    public static String abbreviate(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "μs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new AssertionError();
        }
    }

    public static Duration formatToDuration(String duration) {
        try {
            return Duration.parse(duration);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Duration.parse("PT" + duration.toUpperCase());
        } catch (DateTimeParseException ignored) {
        }

        try {
            long l = toMillis(duration);
            if (l >= 1) {
                return Duration.ofMillis(l);
            }
        } catch (DateTimeParseException ignored) {
        }

        try {
            double v = Double.parseDouble(duration);
            return Duration.ofMillis((long) (v * 1000));
        } catch (Exception ignored) {
        }

        return null;
    }

    public static Period formatToPeriod(String period) {
        try {
            return Period.parse(period);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Period.parse("P" + period.toUpperCase());
        } catch (DateTimeParseException ignored) {
        }

        try {
            long l = toMillis(period);
            if (l >= 1) {
                long days = l / 86400000;
                return Period.ofDays((int) days);
            }
        } catch (DateTimeParseException ignored) {
        }

        try {
            int v = Integer.parseInt(period);
            return Period.ofDays(v);
        } catch (Exception ignored) {
        }

        return null;
    }

}

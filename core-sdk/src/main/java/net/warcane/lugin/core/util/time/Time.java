package net.warcane.lugin.core.util.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitária para manipulação e formatação de tempo.
 *
 * Permite criar objetos de tempo a partir de milissegundos ou outras unidades,
 * converter entre diferentes unidades de tempo e fazer parsing de strings
 * formatadas como "1h 30m 45s".
 *
 * @author Adaptado do código original de Sothatsit
 */
public class Time {

    // Constantes de conversão em milissegundos
    private static final long TICK_MS = 50;          // 1 tick = 50ms (Minecraft)
    private static final long SECOND_MS = 1000;      // 1 segundo
    private static final long MINUTE_MS = SECOND_MS * 60;  // 1 minuto
    private static final long HOUR_MS = MINUTE_MS * 60;    // 1 hora
    private static final long DAY_MS = HOUR_MS * 24;       // 1 dia
    private static final long YEAR_MS = DAY_MS * 365;      // 1 ano (365 dias)

    /**
     * Mapa que associa unidades de tempo (strings) aos seus multiplicadores em milissegundos
     */
    private static final Map<String, Long> UNIT_MULTIPLIERS = new HashMap<>();

    // Pattern para parsing de componentes de tempo (número + unidade) sem espaços obrigatórios
    private static final Pattern TIME_COMPONENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)([a-zA-Z]+)");

    /**
     * Mapa com nomes dos meses em português
     */
    private static final String[] MONTH_NAMES = {
      "janeiro", "fevereiro", "março", "abril", "maio", "junho",
      "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    static {
        // Inicializa as unidades suportadas
        addTimeUnit(1, "ms", "mili", "milis", "milissegundo", "milissegundos");
        addTimeUnit(TICK_MS, "t", "tick", "ticks");
        addTimeUnit(SECOND_MS, "s", "seg", "segundo", "segundos");
        addTimeUnit(MINUTE_MS, "m", "min", "minuto", "minutos");
        addTimeUnit(HOUR_MS, "h", "hora", "horas");
        addTimeUnit(DAY_MS, "d", "dia", "dias");
        addTimeUnit(YEAR_MS, "a", "ano", "anos");
    }

    /**
     * Adiciona uma unidade de tempo ao mapa de multiplicadores
     *
     * @param multiplier O multiplicador em milissegundos
     * @param aliases    Os aliases/nomes aceitos para esta unidade
     */
    private static void addTimeUnit(long multiplier, String... aliases) {
        for (String alias : aliases) {
            UNIT_MULTIPLIERS.put(alias.toLowerCase(), multiplier);
        }
    }

    private final long milliseconds;

    /**
     * Cria um objeto Time a partir de uma quantidade de tempo e sua unidade
     *
     * @param time     A quantidade de tempo
     * @param timeUnit A unidade de tempo
     * @throws IllegalArgumentException se o tempo for negativo
     */
    public Time(long time, TimeUnit timeUnit) {
        this(TimeUnit.MILLISECONDS.convert(time, timeUnit));
    }

    /**
     * Cria um objeto Time a partir de milissegundos
     *
     * @param milliseconds A quantidade de milissegundos
     * @throws IllegalArgumentException se os milissegundos forem negativos
     */
    public Time(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("O número de milissegundos não pode ser negativo: " + milliseconds);
        }
        this.milliseconds = milliseconds;
    }

    /**
     * Converte este tempo para um Instant baseado no epoch
     *
     * @return Instant representando este tempo desde o epoch
     */
    public Instant toInstant() {
        return Instant.ofEpochMilli(milliseconds);
    }

    /**
     * Cria um Instant que representa agora + este tempo
     *
     * @return Instant representando o momento atual mais este tempo
     */
    public Instant toInstantFromNow() {
        return Instant.now().plusMillis(milliseconds);
    }

    /**
     * @return Este tempo em milissegundos
     */
    public long toMilliseconds() {
        return milliseconds;
    }

    /**
     * @return O timestamp atual + este tempo em milissegundos
     */
    public long toActualMilliseconds() {
        return System.currentTimeMillis() + milliseconds;
    }

    /**
     * @return Este tempo em ticks do Minecraft (1 tick = 50ms)
     */
    public double toTicks() {
        return milliseconds / (double) TICK_MS;
    }

    /**
     * @return Este tempo em segundos
     */
    public double toSeconds() {
        return milliseconds / (double) SECOND_MS;
    }

    /**
     * @return Este tempo em minutos
     */
    public double toMinutes() {
        return milliseconds / (double) MINUTE_MS;
    }

    /**
     * @return Este tempo em horas
     */
    public double toHours() {
        return milliseconds / (double) HOUR_MS;
    }

    /**
     * @return Este tempo em dias
     */
    public double toDays() {
        return milliseconds / (double) DAY_MS;
    }

    /**
     * @return Este tempo em anos
     */
    public double toYears() {
        return milliseconds / (double) YEAR_MS;
    }

    /**
     * Converte este tempo para uma string legível em português
     * Exemplo: "1 ano, 2 dias, 3 horas, 30 minutos, 45 segundos"
     *
     * @return Representação em string deste tempo
     */
    @Override
    public String toString() {
        if (milliseconds == 0) {
            return "0 segundos";
        }

        StringBuilder result = new StringBuilder();
        long remaining = milliseconds;

        // Processa cada unidade de tempo da maior para a menor
        remaining = appendTimeUnit(remaining, YEAR_MS, "ano", "anos", result);
        remaining = appendTimeUnit(remaining, DAY_MS, "dia", "dias", result);
        remaining = appendTimeUnit(remaining, HOUR_MS, "hora", "horas", result);
        remaining = appendTimeUnit(remaining, MINUTE_MS, "minuto", "minutos", result);
        remaining = appendTimeUnit(remaining, SECOND_MS, "segundo", "segundos", result);

        // Se ainda sobrar tempo (milissegundos), adiciona
        if (remaining > 0) {
            appendToResult(result, remaining + " ms");
        }

        return result.toString();
    }

    /**
     * Adiciona uma unidade de tempo à string de resultado se aplicável
     *
     * @param time         Tempo restante em milissegundos
     * @param unitMs       Valor da unidade em milissegundos
     * @param singularName Nome singular da unidade
     * @param pluralName   Nome plural da unidade
     * @param result       StringBuilder para construir o resultado
     * @return Tempo restante após subtrair esta unidade
     */
    private long appendTimeUnit(long time, long unitMs, String singularName, String pluralName, StringBuilder result) {
        long units = time / unitMs;
        if (units > 0) {
            String unitName = units == 1 ? singularName : pluralName;
            appendToResult(result, units + " " + unitName);
        }
        return time % unitMs;
    }

    /**
     * Adiciona texto ao resultado com vírgula se necessário
     *
     * @param result StringBuilder para construir o resultado
     * @param text   Texto a ser adicionado
     */
    private void appendToResult(StringBuilder result, String text) {
        if (result.length() > 0) {
            result.append(", ");
        }
        result.append(text);
    }

    /**
     * Faz o parsing de uma string de tempo para criar um objeto Time.
     * <p>
     * Formatos aceitos (SEM ESPAÇOS):
     * - "2h10m50s"
     * - "1d2h30m"
     * - "5s"
     * - "1.5h"
     * <p>
     * Unidades suportadas (case-insensitive):
     * - ms, mili, milis, milissegundo, milissegundos
     * - t, tick, ticks
     * - s, seg, segundo, segundos
     * - m, min, minuto, minutos
     * - h, hora, horas
     * - d, dia, dias
     * - a, ano, anos
     *
     * @param timeString A string a ser processada (sem espaços entre número e unidade)
     * @return Um objeto Time representando o tempo parseado
     * @throws TimeParseException se a string não puder ser processada
     */
    public static Time parseString(String timeString) throws TimeParseException {
        if (timeString == null) {
            throw new IllegalArgumentException("A string de tempo não pode ser nula");
        }

        timeString = timeString.trim();
        if (timeString.isEmpty()) {
            throw new TimeParseException("String de tempo vazia");
        }

        long totalMilliseconds = 0;
        Matcher matcher = TIME_COMPONENT_PATTERN.matcher(timeString);

        boolean foundMatch = false;
        while (matcher.find()) {
            foundMatch = true;
            String numberStr = matcher.group(1);
            String unitStr = matcher.group(2).toLowerCase();

            try {
                double number = Double.parseDouble(numberStr);
                Long multiplier = UNIT_MULTIPLIERS.get(unitStr);

                if (multiplier == null) {
                    throw new TimeParseException("Unidade de tempo desconhecida: \"" + unitStr + "\"");
                }

                totalMilliseconds += (long) (number * multiplier);
            } catch (NumberFormatException e) {
                throw new TimeParseException("Não foi possível converter o número: \"" + numberStr + "\"", e);
            }
        }

        if (!foundMatch) {
            throw new TimeParseException("Nenhum componente de tempo válido encontrado na string: \"" + timeString + "\"");
        }

        return new Time(totalMilliseconds);
    }


    /**
     * Formata um Instant para uma data legível em português.
     * Formato: "17 de agosto de 2025 às 21:51"
     *
     * @param instant O Instant a ser formatado
     * @param zoneId  O fuso horário para conversão (ex: ZoneId.of("America/Sao_Paulo"))
     * @return String formatada da data
     * @throws IllegalArgumentException se instant ou zoneId forem nulos
     */
    public static String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            throw new IllegalArgumentException("Instant não pode ser nulo");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("ZoneId não pode ser nulo");
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);

        int day = dateTime.getDayOfMonth();
        String month = MONTH_NAMES[dateTime.getMonthValue() - 1];
        int year = dateTime.getYear();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();

        return String.format("%d de %s de %d às %02d:%02d", day, month, year, hour, minute);
    }

    /**
     * Formata um Instant para uma data legível em português usando o fuso horário do sistema.
     * <p>
     * Formato: "17 de agosto de 2025 às 21:51"
     *
     * @param instant O Instant a ser formatado
     * @return String formatada da data
     * @throws IllegalArgumentException se instant for nulo
     */
    public static String formatInstant(Instant instant) {
        return formatInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Verifica se dois objetos Time são iguais
     *
     * @param obj O objeto a ser comparado
     * @return true se os objetos representam o mesmo tempo
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Time time = (Time) obj;
        return milliseconds == time.milliseconds;
    }

    /**
     * @return Hash code baseado nos milissegundos
     */
    @Override
    public int hashCode() {
        return Long.hashCode(milliseconds);
    }

    /**
     * Exceção lançada quando há erro no parsing de strings de tempo
     */
    public static class TimeParseException extends RuntimeException {

        /**
         * Cria uma exceção com uma mensagem
         *
         * @param message A mensagem de erro
         */
        public TimeParseException(String message) {
            super(message);
        }

        /**
         * Cria uma exceção com uma mensagem e causa
         *
         * @param message A mensagem de erro
         * @param cause   A causa da exceção
         */
        public TimeParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

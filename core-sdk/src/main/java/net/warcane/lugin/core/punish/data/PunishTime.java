package net.warcane.lugin.core.punish.data;

import lombok.Getter;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
@Getter
public enum PunishTime {

    ONE_HOUR(1, "1 hora"),
    TWO_HOURS(2, "2 horas"),
    SIX_HOURS(6,"6 horas"),
    TWELVE_HOURS(12, "12 horas"),
    ONE_DAY(24,"1 dia"),
    THREE_DAYS(72,"3 dias"),
    ONE_WEEK(168, "7 dias"),
    HALF_MONTH(360,"15 dias"),
    ONE_MONTH(720, "30 dias"),
    TWO_MONTHS(1440, "60 dias"),
    THREE_MONTHS(2160, "90 dias"),
    HALF_YEAR(4320, "180 dias"),
    ONE_YEAR(8760, "1 ano"),
    PERMANENT(-1, "Permanente");

    private final String title;
    private final long timeInMilliseconds;

    PunishTime(int timeInHours, String title) {
        this.title = title;
        this.timeInMilliseconds = (long) timeInHours * 60 * 60 * 1000; // Convert hours to milliseconds
    }
}

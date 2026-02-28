package io.github.minehollow.sdk.stats;

import org.jetbrains.annotations.NotNull;

import java.time.*;
import java.util.Date;

/**
 * Computes the period boundary (expiration) dates for each {@link StatPeriod}.
 * All boundaries are calculated in the server's default timezone.
 */
public final class PeriodBoundary {

    private PeriodBoundary() {}

    /**
     * Returns the expiration {@link Date} for the given period.
     * For {@link StatPeriod#ALLTIME}, returns {@code null} (no expiry).
     */
    public static Date expiresAt(@NotNull StatPeriod period) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        return switch (period) {
            case DAILY -> toDate(today.plusDays(1).atStartOfDay(zone));
            case WEEKLY -> {
                // Next Monday 00:00
                LocalDate nextMonday = today.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
                yield toDate(nextMonday.atStartOfDay(zone));
            }
            case MONTHLY -> toDate(today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone));
            case ALLTIME -> null;
        };
    }

    private static Date toDate(ZonedDateTime zdt) {
        return Date.from(zdt.toInstant());
    }
}


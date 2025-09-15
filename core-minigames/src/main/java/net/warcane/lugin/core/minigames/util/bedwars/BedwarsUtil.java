package net.warcane.lugin.core.minigames.util.bedwars;

import net.warcane.lugin.core.minigames.statistic.PlayerStatistics;
import net.warcane.lugin.core.minigames.util.StatsType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class BedwarsUtil {

    private static final long MILLISECONDS_PER_DAY = TimeUnit.DAYS.toMillis(1);

    private static int getCurrentDay() {
        return (int) (System.currentTimeMillis() / MILLISECONDS_PER_DAY);
    }

    public static void addCoins(@NotNull PlayerStatistics playerStatistics, int value) {
        playerStatistics.addValue(getCurrentDay(), "bedwars_coins", value);
    }

    public static int getCoins(@NotNull PlayerStatistics playerStatistics) {
        return playerStatistics.getTotalValue("bedwars_coins");
    }

    public static void setStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        if (bedwarsMode == BedwarsMode.ALL)
            throw new IllegalArgumentException("Cannot set stats for the ALL mode directly. Set them for individual modes.");

        playerStatistics.setValue(getCurrentDay(), bedwarsMode.getSmallName() + "_" + statsType.name(), value);
    }

    public static void addStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        if (bedwarsMode == BedwarsMode.ALL)
            throw new IllegalArgumentException("Cannot add stats to the ALL mode directly. Add them to a specific mode.");

        int currentDay = getCurrentDay();

        String specificKey = bedwarsMode.getSmallName() + "_" + statsType.name();
        String allKey = BedwarsMode.ALL.getSmallName() + "_" + statsType.name();

        playerStatistics.setValue(currentDay, specificKey, getStatsForDays(playerStatistics, bedwarsMode, statsType, currentDay) + value);
        playerStatistics.setValue(currentDay, allKey, getStatsForDays(playerStatistics, BedwarsMode.ALL, statsType, currentDay) + value);
    }

    public static void removeStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        addStats(playerStatistics, bedwarsMode, statsType, -value);
    }

    public static int getStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType) {
        return playerStatistics.getTotalValue(bedwarsMode.getSmallName() + "_" + statsType.name());
    }

    public static int getStatsForToday(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType) {
        return getStatsForDays(playerStatistics, bedwarsMode, statsType, getCurrentDay());
    }

    public static int getStatsForDays(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int... days) {
        return playerStatistics.getValue(bedwarsMode.getSmallName() + "_" + statsType.name(), days);
    }
}

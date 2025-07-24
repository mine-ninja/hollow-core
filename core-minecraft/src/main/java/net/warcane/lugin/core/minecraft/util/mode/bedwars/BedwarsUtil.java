package net.warcane.lugin.core.minecraft.util.mode.bedwars;

import net.warcane.lugin.core.player.statistic.PlayerStatistics;
import net.warcane.lugin.core.minecraft.util.mode.StatsType;
import org.jetbrains.annotations.NotNull;

public class BedwarsUtil {

    public static void setStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        long millisecondsPerDay = 1000L * 60 * 60 * 24;
        int correspondingDay = (int) (System.currentTimeMillis() / millisecondsPerDay);

        playerStatistics.setValue(correspondingDay, bedwarsMode.getSmallName() + "_" + statsType.name(), value);
    }

    public static void addStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        setStats(playerStatistics, bedwarsMode, statsType, getStats(playerStatistics, bedwarsMode, statsType) + value);
    }

    public static void removeStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int value) {
        setStats(playerStatistics, bedwarsMode, statsType, getStats(playerStatistics, bedwarsMode, statsType) - value);
    }

    public static int getStats(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType) {
                return playerStatistics.getTotalValue(bedwarsMode.getSmallName() + "_" + statsType.name());
    }

    public static int getStatsForToday(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType) {
        long millisecondsPerDay = 1000L * 60 * 60 * 24;
        int correspondingDay = (int) (System.currentTimeMillis() / millisecondsPerDay);

        return getStatsForDays(playerStatistics, bedwarsMode, statsType, correspondingDay);
    }

    public static int getStatsForDays(@NotNull PlayerStatistics playerStatistics, @NotNull BedwarsMode bedwarsMode, @NotNull StatsType statsType, int... days) {
        return playerStatistics.getValue(bedwarsMode.getSmallName() + "_" + statsType.name(), days);
    }
}

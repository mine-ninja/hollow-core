package net.warcane.lugin.core.minigames.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum StatsType {

    GAMES_PLAYED,
    GAMES_WON,
    GAMES_LOST,
    BROKEN_BEDS,
    BEDS_LOST,
    KILLS,
    DEATHS,
    FINAL_KILLS,
    FINAL_DEATHS,
    WINSTREAK,
    MAX_WINSTREAK;

    @Nullable
    public static StatsType getByName(@NotNull String name) {
        for (StatsType statsType : values())
            if (name.equalsIgnoreCase(statsType.name()))
                return statsType;

        return null;
    }
}

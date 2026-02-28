package io.github.minehollow.sdk.stats;

/**
 * The four supported leaderboard periods.
 */
public enum StatPeriod {
    DAILY("stats_daily"),
    WEEKLY("stats_weekly"),
    MONTHLY("stats_monthly"),
    ALLTIME("stats_alltime");

    private final String collectionName;

    StatPeriod(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }
}


 package io.github.minehollow.sdk.stats;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import io.github.minehollow.sdk.database.MongoDbConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Arbitrary statistics service backed by MongoDB.
 * <p>
 * Stores {@code key → long} stats per player across four periods:
 * daily, weekly, monthly, and all-time. Seasonal data expires automatically
 * via MongoDB TTL indexes — no cron jobs needed.
 * <p>
 * Thread-safe. All operations are blocking (call from async context).
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * StatService stats = new StatService();           // uses default MongoDbConnector
 * stats.increment(playerId, "kills", 1);           // +1 across all 4 periods
 * long kills = stats.get(playerId, "kills", StatPeriod.WEEKLY);
 * List<StatEntry> top = stats.leaderboard("kills", StatPeriod.WEEKLY, 10);
 * }</pre>
 */
public class StatService {

    private final Map<StatPeriod, MongoCollection<Document>> collections = new EnumMap<>(StatPeriod.class);

    /**
     * Creates a StatService using the default {@link MongoDbConnector} singleton.
     */
    public StatService() {
        this(MongoDbConnector.getInstance());
    }

    /**
     * Creates a StatService with a specific {@link MongoDbConnector}.
     */
    public StatService(@NotNull MongoDbConnector connector) {
        for (StatPeriod period : StatPeriod.values()) {
            MongoCollection<Document> col = connector.getCollection(period.getCollectionName(), Document.class);
            collections.put(period, col);
        }
        ensureIndexes();
    }

    // ── Indexes ──────────────────────────────────────────────

    private void ensureIndexes() {
        for (StatPeriod period : StatPeriod.values()) {
            MongoCollection<Document> col = collections.get(period);

            // Unique compound index for upserts: { playerId, key }
            col.createIndex(
                Indexes.compoundIndex(Indexes.ascending("playerId"), Indexes.ascending("key")),
                new IndexOptions().unique(true).background(true)
            );

            // Leaderboard index: { key, value DESC }
            col.createIndex(
                Indexes.compoundIndex(Indexes.ascending("key"), Indexes.descending("value")),
                new IndexOptions().background(true)
            );

            // TTL index on expiresAt (only for non-alltime)
            if (period != StatPeriod.ALLTIME) {
                col.createIndex(
                    Indexes.ascending("expiresAt"),
                    new IndexOptions().expireAfter(0L, TimeUnit.SECONDS).background(true)
                );
            }
        }
    }

    // ── Increment ────────────────────────────────────────────

    /**
     * Atomically increments a stat across all four periods.
     *
     * @param playerId the player UUID
     * @param key      the stat key (e.g. "kills", "blocks_mined")
     * @param amount   the amount to increment (can be negative)
     */
    public void increment(@NotNull UUID playerId, @NotNull String key, long amount) {
        for (StatPeriod period : StatPeriod.values()) {
            Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
            List<Bson> updates = new ArrayList<>(2);
            updates.add(Updates.inc("value", amount));

            Date expires = PeriodBoundary.expiresAt(period);
            if (expires != null) {
                updates.add(Updates.setOnInsert("expiresAt", expires));
            }

            collections.get(period).updateOne(filter, Updates.combine(updates), new UpdateOptions().upsert(true));
        }
    }

    /**
     * Atomically increments a stat for a single period.
     */
    public void increment(@NotNull UUID playerId, @NotNull String key, long amount, @NotNull StatPeriod period) {
        Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
        List<Bson> updates = new ArrayList<>(2);
        updates.add(Updates.inc("value", amount));

        Date expires = PeriodBoundary.expiresAt(period);
        if (expires != null) {
            updates.add(Updates.setOnInsert("expiresAt", expires));
        }

        collections.get(period).updateOne(filter, Updates.combine(updates), new UpdateOptions().upsert(true));
    }

    // ── Set ──────────────────────────────────────────────────

    /**
     * Sets a stat to an exact value (overwrites, not cumulative) across all four periods.
     */
    public void set(@NotNull UUID playerId, @NotNull String key, long value) {
        for (StatPeriod period : StatPeriod.values()) {
            setSingle(playerId, key, value, period);
        }
    }

    /**
     * Sets a stat to an exact value for a single period.
     */
    public void set(@NotNull UUID playerId, @NotNull String key, long value, @NotNull StatPeriod period) {
        setSingle(playerId, key, value, period);
    }

    private void setSingle(@NotNull UUID playerId, @NotNull String key, long value, @NotNull StatPeriod period) {
        Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
        List<Bson> updates = new ArrayList<>(2);
        updates.add(Updates.set("value", value));

        Date expires = PeriodBoundary.expiresAt(period);
        if (expires != null) {
            updates.add(Updates.setOnInsert("expiresAt", expires));
        }

        collections.get(period).updateOne(filter, Updates.combine(updates), new UpdateOptions().upsert(true));
    }

    // ── Get ──────────────────────────────────────────────────

    /**
     * Returns the value of a single stat for a player in a given period.
     * Returns {@code 0} if not found.
     */
    public long get(@NotNull UUID playerId, @NotNull String key, @NotNull StatPeriod period) {
        Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
        Document doc = collections.get(period).find(filter).first();
        if (doc == null) return 0L;
        return doc.getLong("value");
    }

    /**
     * Returns all stats for a player in a given period.
     */
    public @NotNull Map<String, Long> getAll(@NotNull UUID playerId, @NotNull StatPeriod period) {
        Bson filter = Filters.eq("playerId", playerId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Document doc : collections.get(period).find(filter)) {
            result.put(doc.getString("key"), doc.getLong("value"));
        }
        return result;
    }

    // ── Leaderboard ──────────────────────────────────────────

    /**
     * Returns the top-N entries for a stat key in a given period, sorted by value descending.
     * Uses the {@code { key, value: -1 }} index — no in-memory sort.
     *
     * @param key    the stat key
     * @param period the leaderboard period
     * @param limit  max entries to return
     * @return ordered list of entries (highest first)
     */
    public @NotNull List<StatEntry> leaderboard(@NotNull String key, @NotNull StatPeriod period, int limit) {
        Bson filter = Filters.eq("key", key);
        Bson sort = Sorts.descending("value");

        List<StatEntry> entries = new ArrayList<>(limit);
        for (Document doc : collections.get(period).find(filter).sort(sort).limit(limit)) {
            UUID playerId = doc.get("playerId", UUID.class);
            long value = doc.getLong("value");
            entries.add(new StatEntry(playerId, key, value));
        }
        return entries;
    }

    /**
     * Returns a player's rank (1-based position) for a stat key in a given period.
     * The rank is the number of players with a strictly higher value plus one.
     * Returns {@code -1} if the player has no entry for that key.
     *
     * @param playerId the player UUID
     * @param key      the stat key
     * @param period   the leaderboard period
     * @return 1-based rank, or -1 if no entry
     */
    public long getRank(@NotNull UUID playerId, @NotNull String key, @NotNull StatPeriod period) {
        long playerValue = get(playerId, key, period);
        if (playerValue == 0L) {
            // Check if an actual document exists with value 0
            Bson exists = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
            if (collections.get(period).countDocuments(exists) == 0) {
                return -1;
            }
        }
        // Count how many players have a higher value
        Bson higher = Filters.and(Filters.eq("key", key), Filters.gt("value", playerValue));
        return collections.get(period).countDocuments(higher) + 1;
    }

    // ── Bulk Increment ───────────────────────────────────────

    /**
     * Atomically increments many stats in a single bulk write per period.
     * Uses unordered bulk writes for maximum throughput.
     *
     * @param playerId   the player UUID
     * @param increments map of stat key → amount to increment
     */
    public void bulkIncrement(@NotNull UUID playerId, @NotNull Map<String, Long> increments) {
        if (increments.isEmpty()) return;

        for (StatPeriod period : StatPeriod.values()) {
            List<WriteModel<Document>> writes = new ArrayList<>(increments.size());
            Date expires = PeriodBoundary.expiresAt(period);

            for (var entry : increments.entrySet()) {
                Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", entry.getKey()));
                List<Bson> updates = new ArrayList<>(2);
                updates.add(Updates.inc("value", entry.getValue()));
                if (expires != null) {
                    updates.add(Updates.setOnInsert("expiresAt", expires));
                }
                writes.add(new UpdateOneModel<>(filter, Updates.combine(updates), new UpdateOptions().upsert(true)));
            }

            collections.get(period).bulkWrite(writes, new BulkWriteOptions().ordered(false));
        }
    }

    /**
     * Bulk increment for a single period.
     */
    public void bulkIncrement(@NotNull UUID playerId, @NotNull Map<String, Long> increments, @NotNull StatPeriod period) {
        if (increments.isEmpty()) return;

        List<WriteModel<Document>> writes = new ArrayList<>(increments.size());
        Date expires = PeriodBoundary.expiresAt(period);

        for (var entry : increments.entrySet()) {
            Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", entry.getKey()));
            List<Bson> updates = new ArrayList<>(2);
            updates.add(Updates.inc("value", entry.getValue()));
            if (expires != null) {
                updates.add(Updates.setOnInsert("expiresAt", expires));
            }
            writes.add(new UpdateOneModel<>(filter, Updates.combine(updates), new UpdateOptions().upsert(true)));
        }

        collections.get(period).bulkWrite(writes, new BulkWriteOptions().ordered(false));
    }

    // ── Delete ───────────────────────────────────────────────

    /**
     * Deletes a single stat for a player across all periods.
     */
    public void delete(@NotNull UUID playerId, @NotNull String key) {
        Bson filter = Filters.and(Filters.eq("playerId", playerId), Filters.eq("key", key));
        for (StatPeriod period : StatPeriod.values()) {
            collections.get(period).deleteOne(filter);
        }
    }

    /**
     * Deletes all stats for a player across all periods.
     */
    public void deleteAll(@NotNull UUID playerId) {
        Bson filter = Filters.eq("playerId", playerId);
        for (StatPeriod period : StatPeriod.values()) {
            collections.get(period).deleteMany(filter);
        }
    }
}


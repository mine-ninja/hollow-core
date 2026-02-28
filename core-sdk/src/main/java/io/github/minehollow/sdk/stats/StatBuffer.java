package io.github.minehollow.sdk.stats;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory buffer for high-frequency stat increments (e.g. block breaks).
 * <p>
 * Accumulates increments locally and flushes to MongoDB via {@link StatService#bulkIncrement}
 * at a configurable interval, reducing write pressure.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * StatBuffer buffer = new StatBuffer(statService);
 * buffer.increment(playerId, "blocks_mined", 1);
 *
 * // Call periodically (e.g. every 5 seconds from an async scheduled task):
 * buffer.flush();
 * }</pre>
 * <p>
 * Thread-safe. {@link #flush()} can be called from any thread.
 */
public class StatBuffer {

    private final StatService statService;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, AtomicLong>> buffer = new ConcurrentHashMap<>();

    public StatBuffer(@NotNull StatService statService) {
        this.statService = statService;
    }

    /**
     * Buffers an increment for later flushing.
     */
    public void increment(@NotNull UUID playerId, @NotNull String key, long amount) {
        buffer.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(key, k -> new AtomicLong())
            .addAndGet(amount);
    }

    /**
     * Flushes all buffered increments to MongoDB via bulk writes, then clears the buffer.
     * <p>
     * Safe to call from any thread. Each player's accumulated increments are written
     * in a single bulk operation per period.
     */
    public void flush() {
        // Snapshot and clear atomically per player
        for (var it = buffer.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            UUID playerId = entry.getKey();
            ConcurrentHashMap<String, AtomicLong> playerStats = entry.getValue();
            it.remove();

            if (playerStats.isEmpty()) continue;

            // Drain the map into a plain map with the accumulated values
            Map<String, Long> increments = new ConcurrentHashMap<>();
            for (var statIt = playerStats.entrySet().iterator(); statIt.hasNext(); ) {
                var statEntry = statIt.next();
                long val = statEntry.getValue().getAndSet(0);
                statIt.remove();
                if (val != 0) {
                    increments.put(statEntry.getKey(), val);
                }
            }

            if (!increments.isEmpty()) {
                statService.bulkIncrement(playerId, increments);
            }
        }
    }

    /**
     * Returns the number of players currently buffered.
     */
    public int bufferedPlayerCount() {
        return buffer.size();
    }

    /**
     * Returns true if the buffer is empty.
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}


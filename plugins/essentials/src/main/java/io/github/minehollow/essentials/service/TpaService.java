package io.github.minehollow.essentials.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages TPA (teleport-ask) requests with expiry.
 * <p>
 * Each player can have at most one incoming request at a time.
 * The key is the <b>target</b> (receiver), the value is the <b>requester</b> (sender).
 */
public class TpaService {

    /** target UUID → requester UUID */
    private final Cache<UUID, UUID> pendingRequests;

    public TpaService(long timeoutSeconds) {
        this.pendingRequests = Caffeine.newBuilder()
            .expireAfterWrite(timeoutSeconds, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
    }

    /**
     * Creates a TPA request from requester to target.
     * Returns false if the requester already has a pending request to this target.
     */
    public boolean createRequest(@NotNull UUID requester, @NotNull UUID target) {
        UUID existing = pendingRequests.getIfPresent(target);
        if (existing != null && existing.equals(requester)) {
            return false; // already pending
        }
        pendingRequests.put(target, requester);
        return true;
    }

    /**
     * Gets and removes the pending request for the target. Returns the requester UUID or null.
     */
    public @Nullable UUID acceptRequest(@NotNull UUID target) {
        UUID requester = pendingRequests.getIfPresent(target);
        if (requester != null) {
            pendingRequests.invalidate(target);
        }
        return requester;
    }

    /**
     * Denies (removes) the pending request for the target. Returns the requester UUID or null.
     */
    public @Nullable UUID denyRequest(@NotNull UUID target) {
        UUID requester = pendingRequests.getIfPresent(target);
        if (requester != null) {
            pendingRequests.invalidate(target);
        }
        return requester;
    }

    /**
     * Gets the pending requester for a target without removing.
     */
    public @Nullable UUID getPendingRequester(@NotNull UUID target) {
        return pendingRequests.getIfPresent(target);
    }

    /**
     * Removes all requests involving a player (both as sender and receiver).
     */
    public void cleanup(@NotNull UUID playerId) {
        pendingRequests.invalidate(playerId);
        // Also clean if they were a requester
        pendingRequests.asMap().entrySet().removeIf(e -> e.getValue().equals(playerId));
    }
}


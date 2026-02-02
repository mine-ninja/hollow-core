package io.github.minehollow.kits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitPlayerData {
    @BsonId
    private String id;
    private UUID playerId;
    private String kitId;
    private long cooldownExpiry;
    private Instant lastClaimed;
    private int totalClaims;

    public KitPlayerData(UUID playerId, String kitId, long cooldownDurationSeconds) {
        this.id = "%s:%s".formatted(playerId, kitId);
        this.playerId = playerId;
        this.kitId = kitId;
        recordClaim(cooldownDurationSeconds);
    }

    public void recordClaim(long cooldownDurationSeconds) {
        this.lastClaimed = Instant.now();
        this.cooldownExpiry = System.currentTimeMillis() + (cooldownDurationSeconds * 1000);
        this.totalClaims++;
    }

    @BsonIgnore
    public boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownExpiry;
    }

    @BsonIgnore
    public long getRemainingSeconds() {
        return Math.max(0, (cooldownExpiry - System.currentTimeMillis()) / 1000);
    }
}

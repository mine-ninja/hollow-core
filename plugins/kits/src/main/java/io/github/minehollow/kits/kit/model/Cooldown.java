package io.github.minehollow.kits.kit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cooldown {

    @BsonId
    private String id;
    private UUID playerId;
    private String kitId;
    private long expiryTime;


    public Cooldown(UUID playerId, String kitId, long expiryTime) {
        this.id = "%s:%s".formatted(playerId, kitId);
        this.playerId = playerId;
        this.kitId = kitId;
        this.expiryTime = expiryTime;
    }

    @BsonIgnore
    public boolean isActive() {
        return System.currentTimeMillis() < expiryTime;
    }

    @BsonIgnore
    public long getRemainingSeconds() {
        return Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000);
    }
}

package io.github.minehollow.ranks.progress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRankProgress {

    @BsonId
    private UUID playerId;

    private int currentRank;
    private double currentExperience;

    private int prestigeLevel;
}

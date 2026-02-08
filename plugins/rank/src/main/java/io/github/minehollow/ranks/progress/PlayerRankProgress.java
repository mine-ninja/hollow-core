package io.github.minehollow.ranks.progress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRankProgress {

    public static PlayerRankProgress createNewProgress(
      @NotNull UUID playerId
    ) {
        return new PlayerRankProgress(playerId, 1, 0, new HashSet<>());
    }

    @BsonId
    private UUID playerId;
    private int currentRank;
    private int prestigeLevel;
    private Set<Integer> levelRewardsClaimed;

    public boolean canClaimLevelReward(int level) {
        return currentRank > level && !levelRewardsClaimed.contains(level);
    }

    public boolean hasClaimedLevelReward(int level) {
        return levelRewardsClaimed.contains(level);
    }

    public void markLevelRewardClaimed(int level) {
        levelRewardsClaimed.add(level);
    }

    public void resetProgress() {
        this.currentRank = 1;
        this.prestigeLevel = 0;
        this.levelRewardsClaimed.clear();
    }
}

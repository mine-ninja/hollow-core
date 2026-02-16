package io.github.minehollow.quests.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerQuestData {
    @BsonId
    private UUID playerId;
    private String lastResetDate;
    private List<ActiveQuest> activeQuests = new ArrayList<>();
    private int claimedToday;
    private int rerolledToday;

    public static PlayerQuestData createNew(@NotNull UUID playerId) {
        return new PlayerQuestData(playerId, LocalDate.now().toString(), new ArrayList<>(), 0, 0);
    }

    public boolean needsReset() {
        if (lastResetDate == null)
            return true;
        return !LocalDate.now().toString().equals(lastResetDate);
    }

    public void resetForNewDay() {
        this.lastResetDate = LocalDate.now().toString();
        this.activeQuests.clear();
        this.claimedToday = 0;
        this.rerolledToday = 0;
    }
}

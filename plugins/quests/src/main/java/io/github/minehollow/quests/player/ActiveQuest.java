package io.github.minehollow.quests.player;

import io.github.minehollow.quests.quest.QuestDifficulty;
import io.github.minehollow.quests.quest.QuestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveQuest {
    private String templateId;
    private int currentProgress;
    private boolean claimed;
    private long assignedAt;
    private int requiredAmount;
    private String difficulty;
    private List<String> rewardCommands = new ArrayList<>();

    public ActiveQuest(String templateId, QuestDifficulty difficulty, QuestType questType) {
        this.templateId = templateId;
        this.currentProgress = 0;
        this.claimed = false;
        this.assignedAt = System.currentTimeMillis();
        this.difficulty = difficulty.name();
        this.requiredAmount = difficulty.randomAmount(questType);
        this.rewardCommands = new ArrayList<>(difficulty.getRewardCommands());
    }

    public void incrementProgress(int amount) {
        this.currentProgress += amount;
    }

    public boolean isCompleted() {
        return currentProgress >= requiredAmount;
    }
}

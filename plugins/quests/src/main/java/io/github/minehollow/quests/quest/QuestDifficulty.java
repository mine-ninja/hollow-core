package io.github.minehollow.quests.quest;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public enum QuestDifficulty {
    EASY(60, Map.of(
            QuestType.BLOCK_BREAK, new int[]{50, 150},
            QuestType.MOB_KILL, new int[]{25, 75},
            QuestType.FISHING, new int[]{10, 30},
            QuestType.PLAY_TIME, new int[]{15, 30}), List.of("eco add %player% rankup_coins 1500")),

    MEDIUM(30, Map.of(
            QuestType.BLOCK_BREAK, new int[]{200, 500},
            QuestType.MOB_KILL, new int[]{100, 200},
            QuestType.FISHING, new int[]{40, 75},
            QuestType.PLAY_TIME, new int[]{35, 60}), List.of("eco add %player% rankup_coins 3250")),

    HARD(10, Map.of(
            QuestType.BLOCK_BREAK, new int[]{600, 1500},
            QuestType.MOB_KILL, new int[]{250, 500},
            QuestType.FISHING, new int[]{100, 150},
            QuestType.PLAY_TIME, new int[]{75, 120}), List.of("give %player% minecraft:diamond 5"));

    @Getter
    private final int weight;
    private final Map<QuestType, int[]> amountRanges;
    @Getter
    private final List<String> rewardCommands;

    QuestDifficulty(int weight, Map<QuestType, int[]> amountRanges, List<String> rewardCommands) {
        this.weight = weight;
        this.amountRanges = amountRanges;
        this.rewardCommands = rewardCommands;
    }

    public int randomAmount(QuestType type) {
        int[] range = amountRanges.get(type);
        if (range == null)
            return 10;

        int raw = ThreadLocalRandom.current().nextInt(range[0], range[1] + 1);
        return roundToMultiple(raw, 5);
    }

    private static int roundToMultiple(int value, int step) {
        return Math.max(step, (value / step) * step);
    }

    public static QuestDifficulty randomWeighted() {
        int totalWeight = 0;
        for (QuestDifficulty d : values()) {
            totalWeight += d.weight;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (QuestDifficulty d : values()) {
            cumulative += d.weight;
            if (roll < cumulative) {
                return d;
            }
        }
        return EASY;
    }
}

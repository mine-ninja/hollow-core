package io.github.minehollow.quests.quest;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public enum QuestType {
    BLOCK_BREAK("Quebrar Blocos", Material.DIAMOND_PICKAXE),
    MOB_KILL("Matar Mobs", Material.DIAMOND_SWORD),
    FISHING("Pescar", Material.FISHING_ROD),
    PLAY_TIME("Tempo Online", Material.CLOCK);

    private final String displayName;
    private final Material icon;

    QuestType(@NotNull String displayName, @NotNull Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }
}

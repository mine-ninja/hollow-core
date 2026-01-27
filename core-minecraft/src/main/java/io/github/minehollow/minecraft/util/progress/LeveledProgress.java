package io.github.minehollow.minecraft.util.progress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeveledProgress {

    public static LeveledProgress createNewProgress() {
        return new LeveledProgress(1, 0);
    }

    private int level;
    private int experience;

    public void addExperience(int experienceToAdd) {
        this.experience += experienceToAdd;
    }

    public void removeExperience(int experienceToRemove) {
        this.experience = Math.max(0, this.experience - experienceToRemove);
    }

    public boolean canLevelUp(int experienceNeeded) {
        return this.experience >= experienceNeeded;
    }
}

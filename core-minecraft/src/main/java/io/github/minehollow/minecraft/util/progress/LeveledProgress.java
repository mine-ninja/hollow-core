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
    private double experience;

    public void addExperience(double experienceToAdd) {
        this.experience += experienceToAdd;
    }

    public void removeExperience(double experienceToRemove) {
        this.experience = Math.max(0, this.experience - experienceToRemove);
    }

    public boolean canLevelUp(double experienceNeeded) {
        return this.experience >= experienceNeeded;
    }

    public void addLevels(int levels) {
        this.level += levels;
    }

    public void removeLevels(int levels) {
        this.level = Math.max(1, this.level - levels);
    }
}



package io.github.minehollow.skills.skill.impl.base;

import io.github.minehollow.skills.skill.Skill;
import org.jetbrains.annotations.NotNull;

public abstract class WeaponUsageSkill extends Skill {

    private final double experiencePerDamage;

    public WeaponUsageSkill(
      @NotNull String id,
      @NotNull String displayName,
      double experiencePerDamage
    ) {
        super(id, displayName);
        this.experiencePerDamage = experiencePerDamage;
    }

    public double getExperiencePerDamage() {
        return experiencePerDamage;
    }
}

package io.github.minehollow.skills.skill.impl.base;

import io.github.minehollow.skills.skill.Skill;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class BlockBreakingSkill extends Skill {

    private final Map<Material , Double> materialXpMap;

    public BlockBreakingSkill(
      @NotNull String id,
      @NotNull String displayName
    ) {
        super(id, displayName);
        this.materialXpMap = new HashMap<>();
    }

    public void addExperienceForMaterial(@NotNull Material material, double xp) {
        materialXpMap.put(material, xp);
    }

    public double getExperienceForMaterial(@NotNull Material material) {
        return materialXpMap.getOrDefault(material, 0.0);
    }

    @NotNull
    public Map<Material, Double> getMaterialXpMap() {
        return materialXpMap;
    }
}

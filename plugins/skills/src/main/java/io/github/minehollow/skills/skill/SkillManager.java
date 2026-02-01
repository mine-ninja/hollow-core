package io.github.minehollow.skills.skill;

import io.github.minehollow.skills.SkillsPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SkillManager {

    private final SkillsPlugin plugin;
    private final Map<String, Skill> skillMap = new HashMap<>();

    public SkillManager(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerSkills(@NotNull Skill... skills) {
        for (Skill skill : skills) {
            skillMap.put(skill.getId(), skill);
            Bukkit.getPluginManager().registerEvents(skill, plugin);
        }
    }

    public @Nullable Skill getSkillById(@NotNull String skillId) {
        return skillMap.get(skillId);
    }

    public Collection<Skill> getAllSkills() {
        return skillMap.values();
    }
}

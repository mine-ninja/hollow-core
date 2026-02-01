package io.github.minehollow.skills.skill;

import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.skill.reward.SkillReward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;

public abstract class Skill implements Listener {


    public static final int MAX_SKILL_LEVEL = 100;

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public static boolean rollChance(double max) {
        double roll = RANDOM.nextDouble(0, 100);
        return roll <= max;
    }

    protected final SkillsPlugin plugin;

    private final String id;
    private final String displayName;
    private final Map<Integer, List<SkillReward>> rewards;

    protected DoubleUnaryOperator levelUpExperienceFunction;
    protected Material icon = Material.BOOK;

    public Skill(@NotNull String id, @NotNull String displayName) {
        this.plugin = JavaPlugin.getPlugin(SkillsPlugin.class);
        this.id = id;
        this.displayName = displayName;
        this.rewards = new HashMap<>();
        this.levelUpExperienceFunction = level -> 100.0 * Math.pow(1.2, level - 1); //
    }

    public @NotNull List<SkillReward> getRewardsForLevel(int level) {
        return this.rewards.getOrDefault(level, Collections.emptyList());
    }

    public @NotNull List<String> getRewardDescription(int level) {
        return this.getRewardsForLevel(level)
          .stream()
          .map(SkillReward::description)
          .toList();
    }

    public void registerNewReward(int level, @NotNull SkillReward reward) {
        this.rewards.computeIfAbsent(level, $ -> new ArrayList<>()).add(reward);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    public int getCurrentLevel(@NotNull Player player) {
        return plugin.getPlayerSkillsProgressController()
          .getSkillLevel(player, this);
    }

    public double getCurrentExperience(@NotNull Player player) {
        return plugin.getPlayerSkillsProgressController()
          .getSkillExperience(player, this);
    }

    protected void addExperience(@NotNull Player player, double experience) {
        plugin.getPlayerSkillsProgressController()
          .addSkillExperience(player, this, experience);
    }

    public double getExperienceToReachLevel(int level) {
        return this.levelUpExperienceFunction.applyAsDouble(level);
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }
}

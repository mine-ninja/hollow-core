package io.github.minehollow.skills.player;

import io.github.minehollow.skills.event.SkillExperienceGainEvent;
import io.github.minehollow.skills.event.SkillLevelUpEvent;
import io.github.minehollow.skills.skill.Skill;
import io.github.minehollow.skills.skill.SkillManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerSkillsProgressController {

    private final SkillManager skillManager;
    private final PlayerSkillsProgressService service;

    public PlayerSkillsProgressController(
      @NotNull SkillManager skillManager,
      @NotNull PlayerSkillsProgressService service
    ) {
        this.skillManager = skillManager;
        this.service = service;
    }

    public int getSkillLevel(
      @NotNull Player player,
      @NotNull String skillId
    ) {
        final var skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Skill with ID " + skillId + " does not exist.");
        }

        return this.getSkillLevel(player, skill);
    }

    public int getSkillLevel(
      @NotNull Player player,
      @NotNull Skill skill
    ) {
        final var progress = service.getCachedPlayerProgress(player.getUniqueId());
        if (progress == null) {
            return 1;
        }

        final var skillProgress = progress.getSkillProgressOrNull(skill.getId());
        return skillProgress != null ? skillProgress.getLevel() : 1;
    }

    public double getSkillExperience(
      @NotNull Player player,
      @NotNull String skillId
    ) {
        final var skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Skill with ID " + skillId + " does not exist.");
        }

        return this.getSkillExperience(player, skill);
    }

    public double getSkillExperience(
      @NotNull Player player,
      @NotNull Skill skill
    ) {
        final var progress = service.getCachedPlayerProgress(player.getUniqueId());
        if (progress == null) {
            return 0;
        }

        final var skillProgress = progress.getSkillProgressOrNull(skill.getId());
        return skillProgress != null ? skillProgress.getExperience() : 0;
    }

    public void addSkillExperience(
      @NotNull Player player,
      @NotNull String skillId,
      double experience
    ) {
        final var skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Skill with ID " + skillId + " does not exist.");
        }

        this.addSkillExperience(player, skill, experience);
    }

    public void addSkillExperience(
      @NotNull Player player,
      @NotNull Skill skill,
      double experience
    ) {
        final var event = SkillExperienceGainEvent.call(player, skill, experience);
        if (event.isCancelled()) {
            return;
        }

        final var progress = service.getCachedPlayerProgress(player.getUniqueId());
        if (progress == null) {
            return;
        }

        final var skillProgress = progress.getSkillProgressOrCreate(skill.getId());
        skillProgress.addExperience(event.getExperienceGain());

        var experienceToLevel = skill.getExperienceToReachLevel(skillProgress.getLevel());
        while (skillProgress.getExperience() >= experienceToLevel) {
            SkillLevelUpEvent.call(
              player,
              skill,
              skillProgress.getLevel(),
              skillProgress.getLevel() + 1
            );

            skillProgress.addLevels(1);
            skillProgress.removeExperience(experienceToLevel);
            experienceToLevel = skill.getExperienceToReachLevel(skillProgress.getLevel());
        }

        service.saveProgress(progress);
    }

    public void addLevel(
      @NotNull Player player,
      @NotNull String skillId,
      int levels
    ) {
        final var skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Skill with ID " + skillId + " does not exist.");
        }

        this.addLevel(player, skill, levels);
    }

    public void addLevel(
      @NotNull Player player,
      @NotNull Skill skill,
      int levels
    ) {
        final var levelUpEvent = SkillLevelUpEvent.call(
          player,
          skill,
          getSkillLevel(player, skill),
          getSkillLevel(player, skill) + levels
        );


        final var progress = service.getCachedPlayerProgress(player.getUniqueId());
        if (progress == null) {
            return;
        }

        final var skillProgress = progress.getSkillProgressOrCreate(skill.getId());
        skillProgress.addLevels(levelUpEvent.getNewLevel());
    }
}

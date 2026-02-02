package io.github.minehollow.skills;

import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.skills.command.SkillsAdminCommand;
import io.github.minehollow.skills.command.SkillsCommand;
import io.github.minehollow.skills.listener.PlayerSkillsNotificationListener;
import io.github.minehollow.skills.menu.SkillLevelListMenu;
import io.github.minehollow.skills.menu.SkillsMainMenu;
import io.github.minehollow.skills.player.PlayerSkillsProgressController;
import io.github.minehollow.skills.player.PlayerSkillsProgressService;
import io.github.minehollow.skills.skill.SkillManager;
import io.github.minehollow.skills.skill.impl.combat.AxeSkill;
import io.github.minehollow.skills.skill.impl.combat.SwordSkill;
import io.github.minehollow.skills.skill.impl.gathering.ExcavationSkill;
import io.github.minehollow.skills.skill.impl.gathering.FarmingSkill;
import io.github.minehollow.skills.skill.impl.gathering.MiningSkill;
import lombok.Getter;

@Getter
public class SkillsPlugin extends SimplePlugin {

    private SkillManager skillManager;

    private PlayerSkillsProgressService playerSkillsProgressService;
    private PlayerSkillsProgressController playerSkillsProgressController;

    @Override
    public void onEnable() {
        playerSkillsProgressService = new PlayerSkillsProgressService(this);

        skillManager = new SkillManager(this);
        skillManager.registerSkills(
          new MiningSkill(),
          new FarmingSkill(),
          new ExcavationSkill(),
          new AxeSkill(),
          new SwordSkill()
        );


        playerSkillsProgressController = new PlayerSkillsProgressController(
          skillManager,
          playerSkillsProgressService
        );

        registerCommands(
          "skills",
          new SkillsAdminCommand(this),
          new SkillsCommand(this)
        );

        MenuUtil.registerMenus(
          new SkillsMainMenu(this),
          new SkillLevelListMenu(this)
        );

        registerListeners(
          new PlayerSkillsNotificationListener(this)
        );
    }
}

package io.github.minehollow.skills.menu;

import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.ProgressBarGenerator;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.skill.Skill;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SkillsMainMenu extends SimpleMenu {

    private static final int[] SKILL_SLOTS = ArrayUtils.addAll(
      IntStream.rangeClosed(20, 24).toArray(),
      IntStream.rangeClosed(29, 33).toArray()
    );

    private final SkillsPlugin plugin;

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        final var player = ctx.getPlayer();

        openHandler.setTitle("Habilidades");
        openHandler.setRows(5);
        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));

        final var skillList = plugin.getSkillManager().getAllSkills().toArray(Skill[]::new);
        final int slotsToUse = Math.min(SKILL_SLOTS.length, skillList.length);

        for (int i = 0; i < slotsToUse; i++) {
            final var skill = skillList[i];
            final var slot = SKILL_SLOTS[i];

            final var icon = this.generateSkillIcon(player, skill);
            ctx.setItem(slot, icon, event -> ctx.openMenu(SkillLevelListMenu.class, Map.of("skill", skill)));
        }

        return true;
    }

    private ItemStack generateSkillIcon(@NotNull Player player, @NotNull Skill skill) {
        final int currentLevel = skill.getCurrentLevel(player);
        final double currentExperience = skill.getCurrentExperience(player);
        final double experienceToNextLevel = skill.getExperienceToReachLevel(currentLevel);

        List<String> nextRewards = skill.getRewardDescription(currentLevel + 1);
        if (nextRewards.isEmpty()) {
            nextRewards = List.of("<gray>Nenhuma recompensa disponível.");
        } else {
            nextRewards = nextRewards.stream()
              .map(line -> "<dark_gray> ○ <white>" + line)
              .toList();
        }

        final String progressBar = ProgressBarGenerator.generateStr(
          currentExperience,
          experienceToNextLevel,
          10,
          '█',
          '█',
          "<green>",
          "<gray>"
        );

        return ItemBuilder.of(skill.getIcon())
          .name("<green>%s <green><bold>%d <green>➡ <green><bold>%d".formatted(
            skill.getDisplayName(),
            currentLevel,
            currentLevel + 1
          ))
          .lore(
            "<gray>",
            "<green> Informações:",
            "<dark_gray> ○ <white>Experiência: <gray>" +
            String.format("%.1f", currentExperience) + "/" +
            String.format("%.1f", experienceToNextLevel) + ".",
            "<dark_gray> ○ <white>Progresso: " + progressBar + " <dark_gray>" +
            String.format("<%.1f", (currentExperience / experienceToNextLevel) * 100) + "%>",
            "<gray>",
            "<green> Próximas Recompensas:"
          )
          .addLore(nextRewards.toArray(new String[0]))
          .addLore("")
          .removeAllFlags()
          .build();
    }
}

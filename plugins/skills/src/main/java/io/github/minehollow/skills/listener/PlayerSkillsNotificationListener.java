package io.github.minehollow.skills.listener;

import io.github.minehollow.minecraft.util.ProgressBarGenerator;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.event.SkillExperienceGainEvent;
import io.github.minehollow.skills.event.SkillLevelUpEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PlayerSkillsNotificationListener implements Listener {


    private static final PredefinedSound LEVEL_UP_SOUND = new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

    // <skillName> <currentLevel> +<experienceGained> XP <progressBar> (<percentage>%)
    private static final String ACTION_BAR_MESSAGE =
      "<white><bold>%skill_symbol%</bold></white> <yellow>Nível %current_level%</yellow> <green>+%experience_gained% XP</green> %progress_bar% <gray>%percentage%%</gray>";

    // Mensagens de level up: notificação clara e celebratória
    private static final String[] LEVEL_UP_MESSAGES = {
      "",
      "<gradient:#FFD700:#FF8C00><bold>SKILL EVOLUÍDA!</bold></gradient>",
      "",
      "<gray>A sua habilidade de </gray><gradient:#FFFFFF:#CCCCCC>%skill_name%</gradient> <gray>subiu de nível</gray> <gradient:#FFD700:#FFA500>%old_level%</gradient> <gray>→</gray> <gradient:#00FF88:#00DD66><bold>%new_level%</bold></gradient>",
      "<gradient:#AAAAAA:#777777>Você ganhou novas habilidades e bônus!</gradient>",
      ""
    };


    private final SkillsPlugin plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSkillLevelUp(@NotNull SkillLevelUpEvent event) {
        final var player = event.getPlayer();
        final int oldLevel = event.getOldLevel();
        final int newLevel = event.getNewLevel();
        final var skill = event.getSkill().getDisplayName();

        for (String levelUpMessage : LEVEL_UP_MESSAGES) {
            final var formattedMessage = StringUtils.text(
              levelUpMessage
                .replace("%skill_name%", skill)
                .replace("%old_level%", String.valueOf(oldLevel))
                .replace("%new_level%", String.valueOf(newLevel))
            );
            player.sendMessage(formattedMessage);
        }

        LEVEL_UP_SOUND.play(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillExperienceGain(@NotNull SkillExperienceGainEvent event) {
        final var player = event.getPlayer();
        final int currentLevel = event.getSkill().getCurrentLevel(player);

        final var experienceToLevel = event.getSkill().getExperienceToReachLevel(currentLevel);
        final double currentExperience = event.getSkill().getCurrentExperience(player);

        final double experienceGained = event.getExperienceGain();
        final var skill = event.getSkill().getDisplayName();

        final var progressBar = ProgressBarGenerator.generateStr(
          currentExperience,
          experienceToLevel,
          10,
          '▪',
          '▪',
          "<green>",
          "<gray>"
        );


        player.sendActionBar(StringUtils.formatString(
          ACTION_BAR_MESSAGE
            .replace("%skill_symbol%", event.getSkill().getTextIcon())
            .replace("%current_level%", String.valueOf(currentLevel))
            .replace("%experience_gained%", String.format("%.2f", experienceGained))
            .replace("%progress_bar%", progressBar)
            .replace("%percentage%", String.format("%.2f", (currentExperience / experienceToLevel) * 100))
        ));

    }
}

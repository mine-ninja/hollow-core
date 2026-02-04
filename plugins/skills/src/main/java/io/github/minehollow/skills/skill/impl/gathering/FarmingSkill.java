package io.github.minehollow.skills.skill.impl.gathering;

import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.skills.skill.impl.base.BlockBreakingSkill;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

import static io.github.minehollow.skills.skill.reward.SkillReward.common;

public class FarmingSkill extends BlockBreakingSkill {

    private static final double DOUBLE_DROP_CHANCE_MULTIPLIER = 0.5;

    public FarmingSkill() {
        super("farming", "Farming");
        this.icon = Material.WHEAT;
        this.textIcon = "⚒";

        addExperienceForMaterial(Material.CACTUS, 1);
        addExperienceForMaterial(Material.WHEAT, 3);
        addExperienceForMaterial(Material.CARROT, 3);
        addExperienceForMaterial(Material.POTATO, 3);
        addExperienceForMaterial(Material.BEETROOT, 5);
        addExperienceForMaterial(Material.BEETROOTS, 5);
        addExperienceForMaterial(Material.PUMPKIN, 8);
        addExperienceForMaterial(Material.CARVED_PUMPKIN, 8);
        addExperienceForMaterial(Material.MELON, 8);
        addExperienceForMaterial(Material.MELON_SLICE, 2);
        addExperienceForMaterial(Material.BROWN_MUSHROOM, 12);
        addExperienceForMaterial(Material.RED_MUSHROOM, 12);
        addExperienceForMaterial(Material.BROWN_MUSHROOM_BLOCK, 15);
        addExperienceForMaterial(Material.RED_MUSHROOM_BLOCK, 15);
        addExperienceForMaterial(Material.MUSHROOM_STEM, 15);
        addExperienceForMaterial(Material.SUGAR_CANE, 20);
        addExperienceForMaterial(Material.NETHER_WART, 40);

        IntStream.rangeClosed(1, MAX_SKILL_LEVEL + 1)
          .forEach(level -> {
              registerNewReward(level, common(
//                level + "% chance de dropar o dobro ao colher."
                String.format("%.1f%% chance de dropar o dobro ao colher.", level * DOUBLE_DROP_CHANCE_MULTIPLIER)
              ));
          });
    }


    @EventHandler(ignoreCancelled = true)
    public void handleBlockBreak(@NotNull BlockBreakEvent event) {
        final var player = event.getPlayer();
        final double experience = getExperienceForMaterial(event.getBlock().getType());
        if (experience <= 0) {
            return;
        }

        final int skillLevel = this.getCurrentLevel(player);
        if (rollChance(skillLevel)) {
            final var itemInHand = player.getInventory().getItemInMainHand();
            event.getBlock().getDrops(itemInHand, player).forEach(drop -> {
                final var duplicatedItem = drop.add(drop.getAmount());
                final int newAmount = duplicatedItem.getAmount();
                if (PlayerUtil.hasSpace(player, duplicatedItem, newAmount)) {
                    player.getInventory().addItem(duplicatedItem);
                } else {
                    event.setCancelled(true);
                    player.sendActionBar(StringUtils.formatString(
                      "<red>Seu inventário está cheio!"
                    ));
                }
            });
        }

        addExperience(player, experience);
    }
}

package io.github.minehollow.skills.skill.impl.gathering;

import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.skills.skill.impl.base.BlockBreakingSkill;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.github.minehollow.skills.skill.reward.SkillReward.common;

public class ExcavationSkill extends BlockBreakingSkill {

    private static final double GLOWSTONE_CHANCE_MULTIPLIER = 0.01;
    private static final double DIAMOND_CHANCE_MULTIPLIER = 0.005;
    private static final double DOUBLE_DROP_CHANCE_MULTIPLIER = 0.01;

    public ExcavationSkill() {
        super("excavation", "Escavação");
        this.icon = Material.IRON_SHOVEL;


        addExperienceForMaterial(Material.DIRT, 1.0);
        addExperienceForMaterial(Material.SAND, 1.0);
        addExperienceForMaterial(Material.GRAVEL, 1.5);
        addExperienceForMaterial(Material.CLAY, 2.0);
        addExperienceForMaterial(Material.SOUL_SAND, 1.5);

        IntStream.range(1, MAX_SKILL_LEVEL)
          .forEach(level -> {
              registerNewReward(level, common(level + "% de chance de dropar o dobro ao escavar."));

              if (level % 5 == 0) {
                  registerNewReward(level, common((int) (level * GLOWSTONE_CHANCE_MULTIPLIER * 100) + "% de chance de encontrar Glowstone ao escavar."));
              }

              if (level % 10 == 0) {
                  registerNewReward(level, common((int) (level * DIAMOND_CHANCE_MULTIPLIER * 100) + "% de chance de encontrar Diamante ao escavar."));
              }
          });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleBlockBreak(@NotNull BlockBreakEvent event) {
        final var player = event.getPlayer();
        final var block = event.getBlock();

        final double experience = getExperienceForMaterial(block.getType());
        if (experience <= 0) {
            return;
        }

        final int skillLevel = getCurrentLevel(player);

        handleDoubleDropReward(event, skillLevel);

        if (skillLevel >= 5) {
            handleGlowstoneReward(event, skillLevel);
        }

        if (skillLevel >= 10) {
            handleDiamondReward(event, skillLevel);
        }

        addExperience(player, experience);
    }

    private void handleDoubleDropReward(@NotNull BlockBreakEvent event, int skillLevel) {
        final double doubleDropChance = skillLevel * DOUBLE_DROP_CHANCE_MULTIPLIER;
        if (!rollChance(doubleDropChance)) {
            return;
        }

        final var player = event.getPlayer();
        final var itemInHand = player.getInventory().getItemInMainHand();
        final var drops = event.getBlock().getDrops(itemInHand, player);

        for (ItemStack drop : drops) {
            final var duplicatedItem = drop.clone();

            if (PlayerUtil.hasSpace(player, duplicatedItem, duplicatedItem.getAmount())) {
                player.getInventory().addItem(duplicatedItem);
                player.sendActionBar(StringUtils.formatString(
                  "<green>✦ Dobro de drops ativado!"
                ));
            } else {
                player.sendActionBar(StringUtils.formatString(
                  "<red>Inventário cheio! Dobro de drops perdido."
                ));
                break;
            }
        }
    }

    private void handleGlowstoneReward(@NotNull BlockBreakEvent event, int skillLevel) {
        final double glowstoneChance = ((double) skillLevel / 5) * GLOWSTONE_CHANCE_MULTIPLIER;

        if (!rollChance(glowstoneChance)) {
            return;
        }

        final var player = event.getPlayer();
        final var glowstoneDrop = new ItemStack(Material.GLOWSTONE_DUST, ThreadLocalRandom.current().nextInt(1, 4));

        if (PlayerUtil.hasSpace(player, glowstoneDrop, glowstoneDrop.getAmount())) {
            player.getInventory().addItem(glowstoneDrop);
        }
    }

    private void handleDiamondReward(@NotNull BlockBreakEvent event, int skillLevel) {
        final double diamondChance = ((double) skillLevel / 10) * DIAMOND_CHANCE_MULTIPLIER;

        if (!rollChance(diamondChance)) {
            return;
        }

        final var player = event.getPlayer();
        final var diamondDrop = new ItemStack(Material.DIAMOND, 1);

        if (PlayerUtil.hasSpace(player, diamondDrop, 1)) {
            player.getInventory().addItem(diamondDrop);
        }
    }
}
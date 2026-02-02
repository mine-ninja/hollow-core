package io.github.minehollow.skills.skill.impl.gathering;

import com.destroystokyo.paper.MaterialTags;
import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.skills.skill.impl.base.BlockBreakingSkill;
import io.github.minehollow.skills.skill.reward.SkillReward;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.stream.IntStream;

public class MiningSkill extends BlockBreakingSkill {

    public MiningSkill() {
        super("mining", "Mineração");
        this.icon = Material.DIAMOND_PICKAXE;
        this.textIcon = "⛏";

        IntStream.range(1, MAX_SKILL_LEVEL)
          .forEach(level -> registerNewReward(level, SkillReward.common((level / 2) + "% de chance de dropar o dobro ao minerar.")));

        addExperienceForMaterial(Material.COAL_ORE, 5.0);
        addExperienceForMaterial(Material.IRON_ORE, 7.0);
        addExperienceForMaterial(Material.GOLD_ORE, 10.0);
        addExperienceForMaterial(Material.DIAMOND_ORE, 15.0);
        addExperienceForMaterial(Material.EMERALD_ORE, 20.0);
        addExperienceForMaterial(Material.NETHER_QUARTZ_ORE, 6.0);
        addExperienceForMaterial(Material.NETHER_GOLD_ORE, 12.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleBlockBreaking(BlockBreakEvent event) {
        final var player = event.getPlayer();
        final var itemInHand = player.getInventory().getItemInMainHand();
        if (!MaterialTags.PICKAXES.isTagged(itemInHand)) {
            return;
        }

        final var skillLevel = this.getCurrentLevel(player);
        final double experienceGained = this.getExperienceForMaterial(event.getBlock().getType());
        if (experienceGained <= 0) {
            return;
        }

        if (rollChance(skillLevel)) {
            for (ItemStack drop : event.getBlock().getDrops(itemInHand, player)) {
                final var duplicatedItem = drop.add(drop.getAmount());
                final int newAmount = duplicatedItem.getAmount();
                if (!PlayerUtil.hasSpace(player, duplicatedItem, newAmount)) {
                    event.setCancelled(true);
                    player.sendActionBar(StringUtils.formatString(
                      "<red>Seu inventário está cheio!"
                    ));
                    break;
                }

                player.getInventory().addItem(duplicatedItem);
            }
        }

        this.addExperience(player, experienceGained);
    }
}

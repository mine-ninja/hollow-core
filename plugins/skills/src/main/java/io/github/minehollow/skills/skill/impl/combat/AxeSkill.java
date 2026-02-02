package io.github.minehollow.skills.skill.impl.combat;

import com.destroystokyo.paper.MaterialTags;
import io.github.minehollow.skills.skill.impl.base.WeaponUsageSkill;
import io.github.minehollow.skills.skill.reward.SkillReward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.stream.IntStream;

public class AxeSkill extends WeaponUsageSkill {

    public AxeSkill() {
        super("axe", "Machado", 0.6);
        this.icon = Material.DIAMOND_AXE;
        this.textIcon = "🪓";
        IntStream.range(1, MAX_SKILL_LEVEL)
          .forEach(level -> registerNewReward(level, SkillReward.common(level + "% de dano adicional com machados.")));
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleDamage(EntityDamageByEntityEvent event) {
        final var damager = event.getDamager();
        if (!(damager instanceof Player player)) return;

        final var itemInHand = player.getInventory().getItemInMainHand();
        if (!MaterialTags.AXES.isTagged(itemInHand)) return;

        final int currentSkillLevel = this.getCurrentLevel(player);

        final double additionalDamage = event.getDamage() * (currentSkillLevel / 100.0); // Each skill level gives 1% additional damage
        event.setDamage(event.getDamage() + additionalDamage);

        final double experienceGain = event.getDamage() * this.getExperiencePerDamage();

        addExperience(player, experienceGain);
    }
}

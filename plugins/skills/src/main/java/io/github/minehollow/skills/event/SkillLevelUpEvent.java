package io.github.minehollow.skills.event;

import io.github.minehollow.skills.skill.Skill;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class SkillLevelUpEvent extends Event {

    @NotNull
    public static SkillLevelUpEvent call(
      @NotNull Player player,
      @NotNull Skill skill,
      int oldLevel,
      int newLevel
    ) {
        SkillLevelUpEvent event = new SkillLevelUpEvent(oldLevel, player, skill, newLevel);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    @Getter
    public static final HandlerList handlerList = new HandlerList();

    private final Player player;
    private final Skill skill;
    private final int oldLevel;
    private int newLevel;

    public SkillLevelUpEvent(int oldLevel, Player player, Skill skill, int newLevel) {
        super(!Bukkit.isPrimaryThread());
        this.oldLevel = oldLevel;
        this.player = player;
        this.skill = skill;
        this.newLevel = newLevel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}

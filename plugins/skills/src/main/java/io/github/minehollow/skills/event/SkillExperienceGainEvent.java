package io.github.minehollow.skills.event;

import io.github.minehollow.skills.skill.Skill;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class SkillExperienceGainEvent extends Event implements Cancellable {

    public static @NotNull SkillExperienceGainEvent call(
      @NotNull Player player,
      @NotNull Skill skill,
      double experienceGain
    ) {
        SkillExperienceGainEvent event = new SkillExperienceGainEvent(player, skill, experienceGain);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final Player player;
    private final Skill skill;
    private double experienceGain;
    private boolean cancelled;

    public SkillExperienceGainEvent(Player player, Skill skill, double experienceGain) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.skill = skill;
        this.experienceGain = experienceGain;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}

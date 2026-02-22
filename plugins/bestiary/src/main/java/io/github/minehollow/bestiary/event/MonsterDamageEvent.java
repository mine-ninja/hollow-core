package io.github.minehollow.bestiary.event;

import io.github.minehollow.bestiary.monster.MonsterManager.ActiveMonster;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MonsterDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final ActiveMonster activeMonster;
    private final double rawDamage;
    private final @Nullable DamageCause cause;

    private double finalDamage;
    private boolean cancelled = false;

    public MonsterDamageEvent(
        @NotNull LivingEntity entity,
        @NotNull ActiveMonster activeMonster,
        double rawDamage,
        @Nullable DamageCause cause
    ) {
        super(!Bukkit.isPrimaryThread());

        this.entity = entity;
        this.activeMonster = activeMonster;
        this.rawDamage = rawDamage;
        this.finalDamage = rawDamage;
        this.cause = cause;
    }

    public @NotNull LivingEntity getEntity() {
        return entity;
    }

    public @NotNull ActiveMonster getActiveMonster() {
        return activeMonster;
    }

    public double getRawDamage() {
        return rawDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public @Nullable DamageCause getCause() {
        return cause;
    }

    public void setFinalDamage(double finalDamage) {
        this.finalDamage = Math.max(0, finalDamage);
    }


    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        this.cancelled = c;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

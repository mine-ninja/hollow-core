package io.github.minehollow.bestiary.event;

import io.github.minehollow.bestiary.monster.MonsterManager.ActiveMonster;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonsterDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final ActiveMonster activeMonster;
    private final @Nullable Player killer;  // null if killed by environment / plugin
    private final List<ItemStack> drops;

    public MonsterDeathEvent(
        @NotNull LivingEntity entity,
        @NotNull ActiveMonster activeMonster,
        @Nullable Player killer,
        @NotNull List<ItemStack> drops
    ) {

        super(!Bukkit.isPrimaryThread());
        this.entity = entity;
        this.activeMonster = activeMonster;
        this.killer = killer;
        this.drops = new ArrayList<>(drops); // defensive copy — listeners mutate freely
    }

    public @NotNull LivingEntity getEntity() {
        return entity;
    }

    public @NotNull ActiveMonster getActiveMonster() {
        return activeMonster;
    }

    public @Nullable Player getKiller() {
        return killer;
    }

    public @NotNull List<ItemStack> getDrops() {
        return drops;
    }

    public @NotNull @Unmodifiable List<ItemStack> getDropsView() {
        return Collections.unmodifiableList(drops);
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

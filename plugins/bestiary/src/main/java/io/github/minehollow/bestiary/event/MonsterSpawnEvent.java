package io.github.minehollow.bestiary.event;

import io.github.minehollow.bestiary.model.CustomMonsterModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MonsterSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CustomMonsterModel model;
    private final int level;
    private Location spawnLocation;
    private boolean cancelled = false;

    public MonsterSpawnEvent(
        @NotNull CustomMonsterModel model,
        int level,
        @NotNull Location spawnLocation) {

        super(!Bukkit.isPrimaryThread());
        this.model = model;
        this.level = spawnLocation != null ? level : 1;
        this.spawnLocation = spawnLocation;
    }


    public @NotNull CustomMonsterModel getModel() {
        return model;
    }

    public int getLevel() {
        return level;
    }

    public @NotNull Location getSpawnLocation() {
        return spawnLocation;
    }


    public void setSpawnLocation(@NotNull Location location) {
        this.spawnLocation = location;
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

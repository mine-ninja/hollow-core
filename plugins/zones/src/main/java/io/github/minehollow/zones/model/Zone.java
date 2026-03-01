package io.github.minehollow.zones.model;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a protected zone in the world.
 */
@Getter
public class Zone {
    private final String id;
    private final ZoneType type;
    private final String world;
    private String displayName;
    private int priority;
    private final ZoneBounds bounds;
    private final Map<ZoneFlag, ZoneFlagState> flags;
    private final ObjectOpenHashSet<UUID> members;

    public Zone(@NotNull String id,
                @NotNull ZoneType type,
                @NotNull String world,
                @NotNull String displayName,
                int priority,
                @NotNull ZoneBounds bounds,
                @NotNull Map<ZoneFlag, ZoneFlagState> flags,
                @NotNull ObjectOpenHashSet<UUID> members) {
        this.id = id;
        this.type = type;
        this.world = world;
        this.displayName = displayName;
        this.priority = priority;
        this.bounds = bounds;
        this.flags = flags;
        this.members = members;
    }

    public boolean contains(@NotNull Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
        return bounds.contains(loc);
    }

    @NotNull
    public ZoneFlagState getFlagState(@NotNull ZoneFlag flag) {
        return flags.getOrDefault(flag, ZoneFlagState.NONE);
    }

    public void setFlag(@NotNull ZoneFlag flag, @NotNull ZoneFlagState state) {
        if (state == ZoneFlagState.NONE) {
            flags.remove(flag);
        } else {
            flags.put(flag, state);
        }
    }

    public boolean isMember(@NotNull UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(@NotNull UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(@NotNull UUID uuid) {
        members.remove(uuid);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }
}


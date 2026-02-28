package io.github.minehollow.npc.api;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface Npc {
    @NotNull String getId();

    @NotNull Location getLocation();

    void teleport(@NotNull Location location);

    void setSkin(@NotNull String value, @NotNull String signature);

    @NotNull String getSkinValue();

    @NotNull String getSkinSignature();

    void setScale(double scale);

    double getScale();

    List<String> getHologramLines();

    void setHologramLine(int index, @NotNull String text);

    void addHologramLine(@NotNull String text);

    void removeHologramLine(int index);

    double getHologramOffset();

    void setHologramOffset(double offset);

    List<NpcAction> getActions();

    void addAction(@NotNull NpcAction action);

    void clearActions();

    NpcClickType getClickType();

    void setClickType(@NotNull NpcClickType type);

    void spawn();

    void despawn();

    boolean isSpawned();

    int getEntityId();
}

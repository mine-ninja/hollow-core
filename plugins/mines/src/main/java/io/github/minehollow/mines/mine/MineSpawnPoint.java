package io.github.minehollow.mines.mine;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MineSpawnPoint(double x, double y, double z, float yaw, float pitch) {

    public static @Nullable MineSpawnPoint readFromSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return new MineSpawnPoint(
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
    }

    public void writeToSection(@NotNull ConfigurationSection root, @NotNull String key) {
        final ConfigurationSection section = root.createSection(key);
        section.set("x", this.x);
        section.set("y", this.y);
        section.set("z", this.z);
        section.set("yaw", this.yaw);
        section.set("pitch", this.pitch);
    }

    public @NotNull Location toLocation(@NotNull World world) {
        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    public static @NotNull MineSpawnPoint fromLocation(@NotNull Location location) {
        return new MineSpawnPoint(
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }
}


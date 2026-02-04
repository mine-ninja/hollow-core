package io.github.minehollow.minecraft.util.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class LocationConfigAdapter {

    public static Location readFromSection(@NotNull ConfigurationSection section) {
        final var worldName = section.getString("world");
        final var x = section.getDouble("x");
        final var y = section.getDouble("y");
        final var z = section.getDouble("z");
        final var yaw = (float) section.getDouble("yaw", 0);
        final var pitch = (float) section.getDouble("pitch", 0);

        if (worldName == null) {
            throw new IllegalArgumentException("World name is missing in the configuration section.");
        }

        final var bukkitWorld = Bukkit.getWorld(worldName);

        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public static void writeToSection(@NotNull ConfigurationSection section, @NotNull Location location) {
        section.set("world", location.getWorld() != null ? location.getWorld().getName() : null);
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());

        final var yaw = location.getYaw();
        if (yaw != 0) {
            section.set("yaw", yaw);
        }

        final var pitch = location.getPitch();
        if (pitch != 0) {
            section.set("pitch", pitch);
        }
    }

}

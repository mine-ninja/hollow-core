package io.github.minehollow.minecraft.util;

import io.github.minehollow.sdk.location.RemoteServerLocation;
import io.github.minehollow.minecraft.BukkitPlatform;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class LocationUtil {

    public static RemoteServerLocation convertToRemoteLocation(@NotNull Location location) {
        final var serverId = BukkitPlatform.getInstance().getId();
        final var worldName = location.getWorld().getName();
        final var x = location.getX();
        final var y = location.getY();
        final var z = location.getZ();
        final var yaw = location.getYaw();
        final var pitch = location.getPitch();

        return new RemoteServerLocation(serverId, worldName, x, y, z, yaw, pitch);
    }

    public static Location transformLocation(@NotNull RemoteServerLocation location) {
        final var world = Bukkit.getWorld(location.worldName());
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + location.worldName());
        }

        final var x = location.x();
        final var y = location.y();
        final var z = location.z();
        final var yaw = location.yaw();
        final var pitch = location.pitch();

        return new Location(world, x, y, z, yaw, pitch);
    }
}

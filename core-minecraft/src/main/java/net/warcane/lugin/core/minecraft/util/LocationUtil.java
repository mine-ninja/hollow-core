package net.warcane.lugin.core.minecraft.util;

import net.warcane.lugin.core.location.RemoteServerLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class LocationUtil {

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

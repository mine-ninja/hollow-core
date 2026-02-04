package io.github.minehollow.mines.model.spawn;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record MineSpawnPosition(
  double x,
  double y,
  double z,
  double yaw,
  double pitch
) {

    public static MineSpawnPosition fromBukkitLocation(@NotNull Location location) {
        return new MineSpawnPosition(
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch()
        );
    }

    public static MineSpawnPosition readFromSection(@NotNull ConfigurationSection section) {
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        double yaw = section.getDouble("yaw");
        double pitch = section.getDouble("pitch");

        return new MineSpawnPosition(x, y, z, yaw, pitch);
    }

    public Map<String, Object> toMap() {
        return Map.of(
          "x", x,
          "y", y,
          "z", z,
          "yaw", yaw,
          "pitch", pitch
        );
    }


    public Location toLocation(@NotNull World world) {
        return new Location(world, x, y, z, (float) yaw, (float) pitch);
    }
}

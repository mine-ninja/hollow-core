package io.github.minehollow.minecraft.util.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleLocation {

    @NotNull
    public static SimpleLocation fromBukkitLocation(@NotNull Location location) {
        return new SimpleLocation(
          location.getWorld().getName(),
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch()
        );
    }

    private String worldName;
    private double x;
    private double y;
    private double z;

    private float yaw;
    private float pitch;


    @NotNull
    public Location toBukkitLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}

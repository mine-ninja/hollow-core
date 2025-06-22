package net.warcane.lugin.core.minecraft.util.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
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

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    private float yaw;
    private float pitch;


    @NotNull
    public Location toBukkitLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}

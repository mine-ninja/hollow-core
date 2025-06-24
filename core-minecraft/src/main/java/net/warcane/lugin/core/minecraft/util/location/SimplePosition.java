package net.warcane.lugin.core.minecraft.util.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplePosition {

    @NotNull
    public static SimplePosition fromBukkitLocation(@NotNull Location location) {
        return new SimplePosition(location.getX(), location.getY(), location.getZ());
    }

    private double x;
    private double y;
    private double z;

    @NotNull
    public Vector toVector() {
        return new Vector(x, y, z);
    }

    @NotNull
    public Location toBukkitLocation(@NotNull World world) {
        return new Location(world, x, y, z);
    }

    @JsonIgnore
    public int getBlockX() {
        return NumberConversions.floor(x);
    }

    @JsonIgnore
    public int getBlockY() {
        return NumberConversions.floor(y);
    }

    @JsonIgnore
    public int getBlockZ() {
        return NumberConversions.floor(z);
    }
}

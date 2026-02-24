package io.github.minehollow.bestiary.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProximityUtil {

    public static int countOnlinePlayersAround(@NotNull World world, int x, int y, int z, double radius) {
        int count = 0;
        double radiusSquared = radius * radius;

        List<Player> players = world.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Location loc = player.getLocation();

            double dx = loc.getX() - x;
            double dy = loc.getY() - y;
            double dz = loc.getZ() - z;

            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
            if (distanceSquared <= radiusSquared) {
                count++;
            }
        }

        return count;
    }
}
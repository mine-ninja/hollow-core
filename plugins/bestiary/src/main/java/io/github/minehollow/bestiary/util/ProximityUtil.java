package io.github.minehollow.bestiary.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ProximityUtil {


    public static Player getNearestPlayer(@NotNull Location center, double radius) {
        final World world = center.getWorld();
        if (world == null) {
            return null;
        }

        Player nearestPlayer = null;
        double nearestDistanceSquared = radius * radius;

        for (final var entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player)) {
                continue;
            }

            final Player player = (Player) entity;
            final Location playerLoc = player.getLocation();

            // Cálculo manual ainda é mais rápido que distanceSquared() devido a overhead
            final double dx = center.getX() - playerLoc.getX();
            final double dy = center.getY() - playerLoc.getY();
            final double dz = center.getZ() - playerLoc.getZ();
            final double distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared < nearestDistanceSquared) {
                nearestPlayer = player;
                nearestDistanceSquared = distanceSquared;
            }
        }

        return nearestPlayer;
    }

    public static boolean hasPlayersAround(@NotNull Location center, double radius) {
        final World world = center.getWorld();
        if (world == null) {
            return false;
        }

        final double radiusSquared = radius * radius;
        final double centerX = center.getX();
        final double centerY = center.getY();
        final double centerZ = center.getZ();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Location playerLoc = player.getLocation();

            if (!world.getUID().equals(playerLoc.getWorld().getUID())) {
                continue;
            }

            final double dx = centerX - playerLoc.getX();
            final double dy = centerY - playerLoc.getY();
            final double dz = centerZ - playerLoc.getZ();

            if ((dx * dx + dy * dy + dz * dz) <= radiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static int countNearbyPlayers(@NotNull Location center, double radius) {
        final World world = center.getWorld();
        if (world == null) {
            return 0;
        }

        final double radiusSquared = radius * radius;
        final double centerX = center.getX();
        final double centerY = center.getY();
        final double centerZ = center.getZ();

        int count = 0;

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Location playerLoc = player.getLocation();

            if (!world.getUID().equals(playerLoc.getWorld().getUID())) {
                continue;
            }

            final double dx = centerX - playerLoc.getX();
            final double dy = centerY - playerLoc.getY();
            final double dz = centerZ - playerLoc.getZ();

            if ((dx * dx + dy * dy + dz * dz) <= radiusSquared) {
                count++;
            }
        }

        return count;
    }
}

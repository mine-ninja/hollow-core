package io.github.minehollow.bestiary.util;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class AsyncEntityUtils {


    @SuppressWarnings("all")
    public static void forEachNearbyEntity(
        @NotNull World world,
        double centerX,
        double centerY,
        double centerZ,
        double radius,
        @Nullable Predicate<Entity> filter,
        @NotNull Consumer<Entity> action
    ) {
        final double radiusSquared = radius * radius;

        try {
            final ServerLevel nmsWorld = ((CraftWorld) world).getHandle();
            final var allEntities = nmsWorld.getAllEntities();

            for (final var nmsEntity : allEntities) {
                if (nmsEntity == null) continue;

                final double ex = nmsEntity.getX();
                final double ey = nmsEntity.getY();
                final double ez = nmsEntity.getZ();

                final double dx = centerX - ex;
                final double dy = centerY - ey;
                final double dz = centerZ - ez;

                final double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq > radiusSquared) continue;

                if (distSq == 0.0) {
                    final var bukkitEntity = nmsEntity.getBukkitEntity();
                    if (bukkitEntity != null && filter == null || filter.test(bukkitEntity)) {
                        action.accept(bukkitEntity);
                    }
                    continue;
                }

                final var bukkitEntity = nmsEntity.getBukkitEntity();
                if (bukkitEntity != null && filter == null || filter.test(bukkitEntity)) {
                    action.accept(bukkitEntity);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get nearby entities using pure NMS method, falling back to Bukkit API. Error: {}", e.getMessage());
        }
    }


    @SuppressWarnings("all")
    public static Entity getNearestEntityPure(
        @NotNull World world,
        double centerX,
        double centerY,
        double centerZ,
        double radius,
        @Nullable Predicate<Entity> filter
    ) {
        final double radiusSquared = radius * radius;
        Entity nearest = null;
        double minDistSquared = radiusSquared;

        try {
            final ServerLevel nmsWorld = ((CraftWorld) world).getHandle();
            final var allEntities = nmsWorld.getAllEntities();

            for (final var nmsEntity : allEntities) {
                if (nmsEntity == null) continue;

                final double ex = nmsEntity.getX();
                final double ey = nmsEntity.getY();
                final double ez = nmsEntity.getZ();

                final double dx = centerX - ex;
                final double dy = centerY - ey;
                final double dz = centerZ - ez;

                final double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq > radiusSquared) continue;

                if (distSq == 0.0) {
                    final var bukkitEntity = nmsEntity.getBukkitEntity();
                    if (bukkitEntity != null && filter == null || filter.test(bukkitEntity)) {
                        return bukkitEntity;
                    }
                    continue;
                }

                if (distSq < minDistSquared) {
                    final var bukkitEntity = nmsEntity.getBukkitEntity();
                    if (bukkitEntity != null && filter == null || filter.test(bukkitEntity)) {
                        nearest = bukkitEntity;
                        minDistSquared = distSq;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get nearby entities using pure NMS method, falling back to Bukkit API. Error: {}", e.getMessage());
            return null;
        }

        return nearest;
    }


    /**
     * Versão async-safe de getNearbyEntities
     * ATENÇÃO: Faz uma cópia snapshot dos chunks para evitar race conditions
     */
    public static Collection<Entity> getNearbyEntitiesAsync(Location location, double x, double y, double z) {
        return getNearbyEntitiesAsync(location, x, y, z, null);
    }

    public static Collection<Entity> getNearbyEntitiesAsync(Location location, double x, double y, double z,
                                                            Predicate<? super Entity> filter) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        BoundingBox aabb = BoundingBox.of(location, x, y, z);
        return getNearbyEntitiesAsync(world, aabb, filter);
    }

    public static Collection<Entity> getNearbyEntitiesAsync(World world, BoundingBox boundingBox) {
        return getNearbyEntitiesAsync(world, boundingBox, null);
    }


    public static Collection<Entity> getNearbyEntitiesAsync(World world, BoundingBox boundingBox,
                                                            Predicate<? super Entity> filter) {
        if (boundingBox == null) {
            throw new IllegalArgumentException("BoundingBox cannot be null");
        }
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }

        // Converte para AABB do NMS
        AABB bb = new AABB(
            boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
            boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
        );

        List<Entity> bukkitEntityList;
        try {
            final var nmsWorld =
                ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
            List<net.minecraft.world.entity.Entity> entityList = nmsWorld.getEntities(null, bb);

            bukkitEntityList = new ArrayList<>(entityList.size());
            for (net.minecraft.world.entity.Entity entity : entityList) {
                Entity bukkitEntity = entity.getBukkitEntity();
                if (bukkitEntity != null) {
                    bukkitEntityList.add(bukkitEntity);
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }


        if (filter != null) {
            bukkitEntityList.removeIf(entity -> !filter.test(entity));
        }

        return bukkitEntityList;
    }
}

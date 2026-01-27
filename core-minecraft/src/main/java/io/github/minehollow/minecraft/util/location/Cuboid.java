package io.github.minehollow.minecraft.util.location;

import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@NoArgsConstructor
@EqualsAndHashCode
@ToString(exclude = {"bukkitMin", "bukkitMax", "bukkitWorld"})
public class Cuboid implements Cloneable, Iterable<Block> {

    public static final int CHUNK_SHIFTS = 4;

    @Setter
    @Getter
    protected String world;

    protected int minX, minY, minZ;

    protected int maxX, maxY, maxZ;

    protected transient Location bukkitMin, bukkitMax;

    protected transient World bukkitWorld;

    public Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);

        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);

        this.bukkitWorld = world;
        this.world = world.getName();

        this.bukkitMin = new Location(bukkitWorld, minX, minY, minZ);
        this.bukkitMax = new Location(bukkitWorld, maxX, maxY, maxZ);
    }

    public Cuboid(Location min, Location max) {
        this(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ(), min.getWorld());
    }

    public Cuboid(Location min, Location max, World world) {
        this(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ(), world);
    }

    public World getBukkitWorld() {
        return Objects.isNull(bukkitWorld) ? Bukkit.getWorld(Objects.isNull(world) ? world = "world" : world) : bukkitWorld;
    }

    public Location getMinLocation() {
        return Objects.isNull(bukkitMin) ? bukkitMin = new Location(getBukkitWorld(), minX, minY, minZ) : bukkitMin;
    }

    public Location getMaxLocation() {
        return Objects.isNull(bukkitMax) ? bukkitMax = new Location(getBukkitWorld(), maxX, maxY, maxZ) : bukkitMax;
    }

    public void getLocations(Consumer<Location> callback) {
        getBlocks((Block block) -> {
            callback.accept(block.getLocation());
        });
    }

    public void getBlocks(Consumer<Block> callback) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    callback.accept(getBukkitWorld().getBlockAt(x, y, z));
                }
            }
        }
    }

    public void getSolidBlocks(final Consumer<Block> callback) {
        getBlocks((Block block) -> {
            if (block != null && block.getType() != Material.AIR) {
                callback.accept(block);
            }
        });
    }

    public void getWalls(final Consumer<Block> callback) {
        World world = getBukkitWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                callback.accept(world.getBlockAt(x, y, minZ));

                if (minZ != maxZ) {
                    callback.accept(world.getBlockAt(x, y, maxZ));
                }
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                callback.accept(world.getBlockAt(minX, y, z));

                if (minX != maxX) {
                    callback.accept(world.getBlockAt(maxX, y, z));
                }
            }
        }
    }

    public void getHollow(final Consumer<Block> callback) {
        getWalls((Block block) -> callback.accept(block));

        World world = getBukkitWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                callback.accept(world.getBlockAt(x, minY, z));

                if (maxY != minY) {
                    callback.accept(world.getBlockAt(x, maxY, z));
                }
            }
        }
    }

    public void getRoof(final Consumer<Block> callBack) {
        getBlocks(block -> {
            if (block.getLocation().getBlockY() == maxY) {
                callBack.accept(block);
            }
        });
    }

    public void getFloor(final Consumer<Block> callBack) {
        getBlocks(block -> {
            if (block.getLocation().getBlockY() == minY) {
                callBack.accept(block);
            }
        });
    }

    public void getBorder(final Consumer<Block> callBack) {
        getBlocks(block -> {
            if (block.getLocation().getBlockY() == maxY) {
                callBack.accept(block);
            }
            if (block.getLocation().getBlockY() == minY) {
                callBack.accept(block);
            }
        });
        getWalls(callBack);
    }

    public void getChunks(BiConsumer<Integer, Integer> callback) {
        for (int x = minX >> CHUNK_SHIFTS; x <= maxX >> CHUNK_SHIFTS; ++x) {
            for (int z = minZ >> CHUNK_SHIFTS; z <= maxZ >> CHUNK_SHIFTS; ++z) {
                callback.accept(x, z);
            }
        }
    }

    public void destroy(boolean removeEntities) {
        getSolidBlocks(block -> block.setType(Material.AIR));

        if (removeEntities) {
            getBukkitWorld().getEntities().stream().filter(entity -> this.contains(entity.getLocation(), true))
              .filter(entity -> !(entity instanceof Player)).forEach(entity -> entity.remove());
        }
    }

    public Location getCenter() {
        double x = (maxX - minX) / 2.0;
        double y = (maxY - minY) / 2.0;
        double z = (maxZ - minZ) / 2.0;

        return new Location(getBukkitWorld(), minX + x, minY + y, minZ + z);
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getLength() {
        return maxZ - minZ + 1;
    }

    public int getHeigth() {
        return maxY - minY + 1;
    }

    public int getSize() {
        return getWidth() * getLength() * getHeigth();
    }

    public boolean contains(int x, int y, int z) {
        return (x >= minX && x <= maxX) && (y >= minY && y <= maxY) && (z >= minZ && z <= maxZ);
    }

    public boolean contains(Location location, boolean sameWorld) {
        if (Objects.isNull(location)) return false;

        if (sameWorld && !location.getWorld().getName().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return contains(x, y, z);
    }

    public Cuboid expand(int x, int y, int z) {
        return new Cuboid(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z, getBukkitWorld());
    }

    public Cuboid contract(int x, int y, int z) {
        return expand(-x, -y, -z);
    }

    @Override
    public Cuboid clone() {
        return new Cuboid(minX, minY, minZ, maxX, maxY, maxZ, getBukkitWorld());
    }

    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {

            private int nextX = minX;
            private int nextY = minY;
            private int nextZ = minZ;

            @Override
            public boolean hasNext() {
                return nextX != Integer.MIN_VALUE;
            }

            @Override
            public Block next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }

                Block block = getBukkitWorld().getBlockAt(nextX, nextY, nextZ);
                if (++nextX > maxX) {
                    nextX = minX;
                    if (++nextY > maxY) {
                        nextY = minY;
                        if (++nextZ > maxZ) {
                            nextX = Integer.MIN_VALUE;
                        }
                    }
                }
                return block;
            }
        };
    }

}
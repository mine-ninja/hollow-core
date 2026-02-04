package io.github.minehollow.mines.model;

import io.github.minehollow.minecraft.util.ChunkUtil;
import io.github.minehollow.mines.model.block.MineBlockConfig;
import io.github.minehollow.mines.model.icon.MineMenuIcon;
import io.github.minehollow.mines.model.spawn.MineSpawnPosition;
import io.github.minehollow.mines.util.MinePreSelection;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class Mine {

    public static final int DEFAULT_RESET_INTERVAL_SECONDS = 180; // 3 minutes

    private static final int RESET_PERCENTAGE_THRESHOLD = 20; // 20% of the mine's volume

    // Time to consider the mine "idle" for auto-reset purposes
    private static final long MAX_IDLE_TIME_MILLIS = 5 * 60 * 1000;

    public static Mine createDefaultMine(
      @NotNull String id,
      @NotNull Player player,
      @NotNull MinePreSelection selection
    ) {
        final var chunks = selection.getAllChunksInSelection();

        final int minX = selection.getMinX();
        final int minY = selection.getMinY();
        final int maxY = selection.getMaxY();

        final var spawnPosition = MineSpawnPosition.fromBukkitLocation(player.getLocation());
        final var menuIcon = MineMenuIcon.createDefault();
        final var blockConfigs = new HashMap<Material, MineBlockConfig>();
        blockConfigs.put(Material.STONE, MineBlockConfig.createDefault(Material.STONE));

        return Mine.builder()
          .id(id)
          .spawnPosition(spawnPosition)
          .menuIcon(menuIcon)
          .chunks(chunks)
          .minX(minX)
          .minY(minY)
          .minZ(selection.getMinZ())
          .maxX(selection.getMaxX())
          .maxY(maxY)
          .maxZ(selection.getMaxZ())
          .blockConfigs(blockConfigs)
          .metadata(new HashMap<>())
          .build();
    }


    private String id;
    private MineSpawnPosition spawnPosition;
    private MineMenuIcon menuIcon;

    private LongSet chunks;

    private int minX;
    private int minY;
    private int minZ;

    private int maxX;
    private int maxY;
    private int maxZ;


    private Map<Material, MineBlockConfig> blockConfigs;
    private Map<String, Object> metadata;

    private final MutableInt airBlocksCount = new MutableInt(0);
    private final MutableLong lastBlockBreakTime = new MutableLong(System.currentTimeMillis());


    public boolean containsBlock(@NotNull String worldName, @NotNull int x, int y, int z) {
        final var chunkHash = ChunkUtil.pack(x >> 4, z >> 4);
        return chunks.contains(chunkHash)
               && y <= maxY
               && y >= minY;
    }

    public MineStatistics toStatistics() {
        return new MineStatistics(id, chunks.size());
    }

    public boolean hasValidYBounds() {
        return minY >= 0 && maxY >= 0 && maxY > minY;
    }

    public record MineStatistics(
      @NotNull String id,
      int chunkCount
    ) {
    }

    @ApiStatus.Internal
    public List<String> serializeChunks() {
        final var list = new ArrayList<String>(chunks.size());
        for (long chunkHash : chunks) {
            list.add(Long.toString(chunkHash));
        }
        return list;
    }

    public boolean isInsideArea(int x, int y, int z) {
        return x >= minX && x <= maxX
               && y >= minY && y <= maxY
               && z >= minZ && z <= maxZ;
    }

    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public void incrementAirBlocks(int delta) {
        airBlocksCount.add(delta);
    }

    public boolean canReset() {
        int totalVolume = getVolume();
        int currentAirBlocks = airBlocksCount.getValue();
        int percentageAir = (currentAirBlocks * 100) / totalVolume;
        return percentageAir >= RESET_PERCENTAGE_THRESHOLD;
    }

    public boolean isIdle() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastBlockBreakTime.getValue()) >= MAX_IDLE_TIME_MILLIS;
    }

    public void resetAirCount() {
        airBlocksCount.setValue(0);
    }

    public void updateLastBlockBreakTime() {
        lastBlockBreakTime.setValue(System.currentTimeMillis());
    }
}
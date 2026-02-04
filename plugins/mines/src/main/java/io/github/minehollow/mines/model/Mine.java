package io.github.minehollow.mines.model;

import io.github.minehollow.minecraft.util.ChunkUtil;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.mines.model.block.MineBlockConfig;
import io.github.minehollow.mines.model.icon.MineMenuIcon;
import io.github.minehollow.mines.model.spawn.MineSpawnPosition;
import io.github.minehollow.mines.util.MinePreSelection;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Mine {

    public static final int DEFAULT_RESET_INTERVAL_SECONDS = 180; // 3 minutes

    public static Mine createDefaultMine(
      @NotNull String id,
      @NotNull Player player,
      @NotNull MinePreSelection selection
    ) {
        final var chunks = selection.getAllChunksInSelection();

        final int minY = selection.getMinY();
        final int maxY = selection.getMaxY();

        final var spawnPosition = MineSpawnPosition.fromBukkitLocation(player.getLocation());
        final var menuIcon = MineMenuIcon.createDefault();
        final var blockConfigs = new HashMap<Material, MineBlockConfig>();
        blockConfigs.put(Material.STONE, MineBlockConfig.createDefault(Material.STONE));

        return new Mine(id, spawnPosition, menuIcon, chunks, minY, maxY, blockConfigs, new HashMap<>());
    }


    private String id;
    private MineSpawnPosition spawnPosition;
    private MineMenuIcon menuIcon;

    private LongSet chunks;

    private int minY;
    private int maxY;


    private Map<Material, MineBlockConfig> blockConfigs;
    private Map<String, Object> metadata;

    private final Stopwatch resetStopwatch = new Stopwatch();


    public boolean containsBlock(@NotNull String worldName, @NotNull int x, int y, int z) {
        final var chunkHash = ChunkUtil.pack(x >> 4, z >> 4);
        return chunks.contains(chunkHash)
               && y <= maxY
               && y >= minY;
    }

    public MineStatistics toStatistics() {
        return new MineStatistics(id, chunks.size());
    }

    public boolean canReset() {
        return resetStopwatch.hasElapsedSeconds(DEFAULT_RESET_INTERVAL_SECONDS);
    }

    public void resetTimer() {
        resetStopwatch.reset();
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
}
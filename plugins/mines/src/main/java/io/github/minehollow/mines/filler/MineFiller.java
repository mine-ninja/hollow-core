package io.github.minehollow.mines.filler;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.github.minehollow.minecraft.util.ChunkUtil;
import io.github.minehollow.mines.model.Mine;
import io.github.minehollow.mines.model.MineManager;
import io.github.minehollow.mines.model.block.MineBlockConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Slf4j
public class MineFiller {

    public static void fillMine(
      @NotNull MineManager mineManager,
      @NotNull Mine mine,
      @Nullable Runnable runnable
    ) {
        Thread.startVirtualThread(() -> {
            World world = Bukkit.getWorld(mineManager.getMineWorldName());
            if (world == null) {
                log.error("Mundo da mina não encontrado: " + mineManager.getMineWorldName());
                return;
            }

            final RandomPattern pattern = generatePattern(mine);
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                session.setMask(new BlockTypeMask(session, BlockTypes.AIR));
                final LongIterator longIterator = mine.getChunks().longIterator();

                while (longIterator.hasNext()) {
                    final long chunkLong = longIterator.nextLong();
                    final int chunkX = ChunkUtil.unpackX(chunkLong);
                    final int chunkZ = ChunkUtil.unpackZ(chunkLong);
                    fillMineChunk(session, pattern, world, mine, chunkX, chunkZ);
                }

                // Flush final para garantir
                session.flushQueue();

            } catch (Exception e) {
                log.error("Erro ao preencher a mina: " + mine.getId(), e);
                e.printStackTrace();
            } finally {
                mine.resetTimer();
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }


    /**
     * Preenche uma única chunk da mina.
     *
     * @return Número de blocos alterados
     */
    public static int fillMineChunk(
      @NotNull EditSession session,
      @NotNull RandomPattern pattern,
      @NotNull World world,
      @NotNull Mine mine,
      int chunkX,
      int chunkZ
    ) {
        int minMineY = mine.getMinY();
        int maxMineY = mine.getMaxY();

        // Converter coordenadas de CHUNK para coordenadas de BLOCO
        // Uma chunk tem 16x16 blocos
        int blockMinX = chunkX * 16;
        int blockMinZ = chunkZ * 16;
        int blockMaxX = blockMinX + 15;
        int blockMaxZ = blockMinZ + 15;

        // Criar os vetores para a região cúbica
        BlockVector3 min = BlockVector3.at(blockMinX, minMineY, blockMinZ);
        BlockVector3 max = BlockVector3.at(blockMaxX, maxMineY, blockMaxZ);

        Region region = new CuboidRegion(
          BukkitAdapter.adapt(world),
          min,
          max
        );

        try {
            int blocksChanged = session.setBlocks(region, pattern);


            if (blocksChanged == 0) {
                log.warn("ATENÇÃO: Nenhum bloco foi alterado na chunk ({}, {})!", chunkX, chunkZ);
                log.warn("  Região: Min({}, {}, {}) Max({}, {}, {})",
                  blockMinX, minMineY, blockMinZ, blockMaxX, maxMineY, blockMaxZ);
            }

            return blocksChanged;

        } catch (Exception e) {
            log.error("Erro ao preencher chunk ({}, {}) da mina {}", chunkX, chunkZ, mine.getId(), e);
            return 0;
        }
    }

    private static RandomPattern generatePattern(@NotNull Mine mine) {
        RandomPattern randomPattern = new RandomPattern();
        if (mine.getBlockConfigs().isEmpty()) {
            log.error("ERRO CRÍTICO: Nenhum bloco configurado na mina {}!", mine.getId());
            return randomPattern;
        }

        double totalChance = 0.0;
        for (Map.Entry<Material, MineBlockConfig> entry : mine.getBlockConfigs().entrySet()) {
            double chance = entry.getValue().getSpawnChance();
            if (chance > 0) {
                BlockState blockState = BukkitAdapter.adapt(entry.getKey().createBlockData());
                randomPattern.add(blockState, chance);
                totalChance += chance;
            } else {
                log.warn("  ✗ {} ignorado (chance = 0)", entry.getKey());
            }
        }

        if (totalChance == 0) {
            log.error("ERRO CRÍTICO: Pattern vazio! Nenhum bloco com spawn-chance > 0!");
        }

        return randomPattern;
    }
}
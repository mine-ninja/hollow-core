package io.github.minehollow.mines.filler;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import io.github.minehollow.mines.model.Mine;
import io.github.minehollow.mines.model.MineManager;
import io.github.minehollow.mines.model.block.MineBlockConfig;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MineFiller {

    private static final Map<String, RandomPattern> patternCache = new ConcurrentHashMap<>();


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
                final var pos1 = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ());
                final var pos2 = BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ());

                final Region region = new CuboidRegion(BukkitAdapter.adapt(world), pos1, pos2);

                session.setBlocks(region, pattern);
                session.flushQueue();
            } catch (Exception e) {
                log.error("Erro ao preencher a mina: " + mine.getId(), e);
            } finally {
                mine.resetAirCount();
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    public static void invalidatePatternCache(@NotNull Mine mine) {
        patternCache.remove(mine.getId());
    }

    public static void clearPatternCache() {
        patternCache.clear();
    }

    private static RandomPattern generatePattern(@NotNull Mine mine) {
        final var cached = patternCache.get(mine.getId());
        if (cached != null) {
            return cached;
        }

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

        patternCache.put(mine.getId(), randomPattern);
        return randomPattern;
    }
}
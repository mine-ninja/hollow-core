package io.github.minehollow.mines.model;

import io.github.minehollow.minecraft.util.ChunkUtil;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.filler.MineFiller;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Getter
@RequiredArgsConstructor
public class MineManager {

    private final MinesPlugin plugin;

    private String mineWorldName;

    private final Map<String, Mine> mines = new HashMap<>();
    private final Long2ObjectMap<String> blockToMineIdMap = new Long2ObjectOpenHashMap<>();

    public void initializeMines() {
        mines.clear();
        blockToMineIdMap.clear();
        MineFiller.clearPatternCache();

        final var config = plugin.getConfig();
        this.mineWorldName = config.getString("mine-world-name", "world");

        ConfigurationSection minesSection = config.getConfigurationSection("mines");
        if (minesSection == null) {
            log.warn("No 'mines' section found in the configuration.");
            return;
        }

        for (String mineId : minesSection.getKeys(false)) {
            ConfigurationSection mineSection = minesSection.getConfigurationSection(mineId);
            if (mineSection != null) {
                Mine mine = MineConfigAdapter.readFromSection(mineSection);
                this.cacheMine(mine);
            }
        }

        Bukkit.getConsoleSender().sendMessage("§a[MinesPlugin] Loaded " + mines.size() + " mines.");
    }

    public void saveMines() {
        final var config = plugin.getConfig();
        config.set("mines", null); // Limpa para evitar duplicatas ou deletadas
        ConfigurationSection minesSection = config.createSection("mines");

        mines.forEach((id, mine) ->
          MineConfigAdapter.writeToSection(mine, minesSection)
        );

        plugin.saveConfig(); // Grava no disco
    }

    public void setMineWorldName(String worldName) {
        this.mineWorldName = worldName;
        plugin.getConfig().set("mine-world-name", worldName);
        plugin.saveConfig();
    }

    public void addAndSaveMine(String id, Mine mine) {
        this.mines.put(id, mine);
        saveMines();
    }

    public void cacheMine(@NotNull Mine mine) {
        mines.put(mine.getId().toLowerCase(), mine);

        LongIterator longIterator = mine.getChunks().longIterator();
        while (longIterator.hasNext()) {
            long chunkKey = longIterator.nextLong();
            blockToMineIdMap.put(chunkKey, mine.getId().toLowerCase());
        }
    }

    public void forEachPlayersInsideMine(@NotNull Consumer<Player> action) {
        final var bukkitWorld = Bukkit.getWorld(mineWorldName);
        if (bukkitWorld == null) {
            log.warn("Mundo das minas não encontrado: " + mineWorldName);
            return;
        }

        for (Player player : bukkitWorld.getPlayers()) {
            Mine mine = getMineAt(player.getLocation().getBlock());
            if (mine != null) {
                action.accept(player);
            }
        }
    }

    @Nullable
    public Mine getMineAt(int x, int y, int z) {
        final int chunkX = x >> 4; // x dividido por 16
        final int chunkZ = z >> 4;

        final long hash = ChunkUtil.pack(chunkX, chunkZ);
        final String mineId = blockToMineIdMap.get(hash);
        if (mineId == null) {
            return null;
        }

        final var mine = mines.get(mineId);
        if (mine != null && mine.isInsideArea(x, y, z)) {
            return mine;
        }

        return null;
    }

    @Nullable
    public Mine getMineAt(@NotNull Block block) {
        return getMineAt(block.getLocation());
    }

    @Nullable
    public Mine getMineAt(@NotNull Location location) {
        if (!location.getWorld().getName().equals(mineWorldName)) {
            return null;
        }

        return getMineAt(
          location.getBlockX(),
          location.getBlockY(),
          location.getBlockZ()
        );
    }

    @Nullable
    public Mine getMineById(@NotNull String id) {
        return mines.get(id.toLowerCase());
    }

    @NotNull
    public Map<String, Mine> getMines() {
        return mines;
    }
}
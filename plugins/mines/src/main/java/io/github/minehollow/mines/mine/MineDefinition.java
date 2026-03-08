package io.github.minehollow.mines.mine;

import io.github.minehollow.mines.pallet.LeveledBlockPalette;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Representa a definição de uma mina, incluindo suas áreas, blocos e outras propriedades. é necessário para criar uma mina virtual para um jogador.
 */
@Slf4j
@Data
@AllArgsConstructor
public final class MineDefinition {

    private String id;
    private String displayName;
    private @Nullable String headUrl;
    // Area da warp da mina, onde os jogadores serão teleportados ao entrar na mina.
    private SimpleCuboidArea globalArea;
    // Area onde de fato os jogadores vao quebrar blocos.
    private @Nullable SimpleCuboidArea miningArea;
    // Ponto de spawn opcional para entrada na mina.
    private @Nullable MineSpawnPoint spawnPoint;
    // Blocos que serão renderizados na mina
    // dependendo do nível da mina do jogador.
    private LeveledBlockPalette blockPalette;
    // Valores de moedas que ganha ao quebrar blocos.
    private Object2LongMap<String> currencyGainValues;


    public static MineDefinition readFromSection(@NotNull ConfigurationSection section) {
        final String id = section.getName();
        final String displayName = section.getString("display-name", id);
        final String headUrl = section.getString("head-url");
        final SimpleCuboidArea globalArea = SimpleCuboidArea.readFromSection(section.getConfigurationSection("global-area"));

        final ConfigurationSection miningAreaSection = section.getConfigurationSection("mining-area");
        final SimpleCuboidArea miningArea = miningAreaSection == null
            ? null
            : SimpleCuboidArea.readFromSection(miningAreaSection);

        final MineSpawnPoint spawnPoint = MineSpawnPoint.readFromSection(section.getConfigurationSection("spawn-point"));

        ConfigurationSection paletteSection = section.getConfigurationSection("block-palette");
        if (paletteSection == null) {
            // Compatibilidade com configs antigas.
            paletteSection = section.getConfigurationSection("block-palettes");
        }

        final LeveledBlockPalette blockPalette = LeveledBlockPalette.readFromSection(paletteSection);


        final Object2LongMap<String> currencyGainValues = new Object2LongArrayMap<>();
        ConfigurationSection gainValuesSection = section.getConfigurationSection("currency-gain-values");
        if (gainValuesSection != null) {
            for (String key : gainValuesSection.getKeys(false)) {
                final long value = gainValuesSection.getLong(key, 0);
                if (value > 0) {
                    currencyGainValues.put(key, value);
                } else {
                    log.warn("Invalid currency gain value '{}' for key '{}' in mine '{}'", value, key, id);
                }
            }
        }

        return new MineDefinition(id, displayName, headUrl, globalArea, miningArea, spawnPoint, blockPalette, currencyGainValues);
    }


    public void writeToSection(@NotNull ConfigurationSection root, @NotNull String key) {
         final ConfigurationSection section = root.createSection(key);
         section.set("display-name", this.displayName);
         section.set("head-url", this.headUrl);
         this.globalArea.writeToSection(section, "global-area");
         if (this.miningArea != null) {
             this.miningArea.writeToSection(section, "mining-area");
         }
         if (this.spawnPoint != null) {
             this.spawnPoint.writeToSection(section, "spawn-point");
         }
         this.blockPalette.writeToSection(section, "block-palette");

        if (this.currencyGainValues != null && !this.currencyGainValues.isEmpty()) {
            final ConfigurationSection gainValuesSection = section.createSection("currency-gain-values");
            for (Object2LongMap.Entry<String> entry : this.currencyGainValues.object2LongEntrySet()) {
                final long value = entry.getLongValue();
                if (value > 0L) {
                    gainValuesSection.set(entry.getKey(), value);
                }
            }
        }
     }
 }

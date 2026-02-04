package io.github.minehollow.mines.model.block;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
public class MineBlockConfig {

    private final Material material;
    private final Map<String, Double> currencyValues;
    private final double spawnChance;
    private final double minExperienceReward;
    private final double maxExperienceReward;

    public static MineBlockConfig createDefault(@NotNull Material material) {
        return createNew(material, Map.of("rankup_coins", 1.0), 100.0, 0.0, 0.0);
    }

    /**
     * Lê todas as configurações de blocos de uma seção raiz.
     * Corrigido para filtrar materiais inválidos e evitar erros de interrupção.
     */
    public static Map<Material, MineBlockConfig> readAllFromSection(
      @NotNull ConfigurationSection root
    ) {
        Map<Material, MineBlockConfig> configs = new HashMap<>();
        for (String key : root.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                MineBlockConfig config = readFromSection(root, material);
                if (config != null) {
                    configs.put(material, config);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Material inválido na configuração de blocos: {}", key);
            }
        }
        return configs;
    }

    /**
     * Lê a configuração de um material específico.
     */
    public static @Nullable MineBlockConfig readFromSection(@NotNull ConfigurationSection root, @NotNull Material material) {
        final ConfigurationSection section = root.getConfigurationSection(material.name());
        if (section == null) return null;
        return readFromSection(section);
    }

    /**
     * Corrigido para processar os valores de moeda sem redundância de caminhos.
     */
    public static MineBlockConfig readFromSection(@NotNull ConfigurationSection section) {
        Material material;
        try {
            material = Material.valueOf(section.getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        ConfigurationSection chanceSection = section.getConfigurationSection("currency-values");
        Map<String, Double> currencyChances = (chanceSection == null)
          ? Collections.emptyMap()
          : parseCurrencyValues(chanceSection);

        return createNew(
          material,
          currencyChances,
          section.getDouble("spawn-chance", 100.0),
          section.getDouble("min-experience-reward", 0.0),
          section.getDouble("max-experience-reward", 0.0)
        );
    }

    public static MineBlockConfig createNew(Material m, Map<String, Double> c, double s, double min, double max) {
        return new MineBlockConfig(m, c, s, min, max);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("spawn-chance", this.spawnChance);
        map.put("min-experience-reward", this.minExperienceReward);
        map.put("max-experience-reward", this.maxExperienceReward);
        map.put("currency-values", this.currencyValues);
        return map;
    }

    /**
     * Corrigido: Agora lê diretamente da seção de chances sem concatenar o prefixo redundante.
     */
    private static Map<String, Double> parseCurrencyValues(ConfigurationSection chanceSection) {
        Map<String, Double> values = new HashMap<>();
        for (String key : chanceSection.getKeys(false)) {
            values.put(key, chanceSection.getDouble(key));
        }
        return values;
    }
}
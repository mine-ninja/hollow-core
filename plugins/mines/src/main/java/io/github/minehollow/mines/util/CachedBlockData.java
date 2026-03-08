package io.github.minehollow.mines.util;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedBlockData {

    private static final Map<Material , BlockData> CACHE = new ConcurrentHashMap<>();

    public static @NotNull BlockData get(Material material) {
        return CACHE.computeIfAbsent(material, Material::createBlockData);
    }
}

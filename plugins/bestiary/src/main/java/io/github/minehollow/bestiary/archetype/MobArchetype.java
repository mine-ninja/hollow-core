package io.github.minehollow.bestiary.archetype;

import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.range.IntRange;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record MobArchetype(
    @NotNull @BsonId String id,
    @NotNull String displayName,
    @NotNull EntityType entityType,
    @NotNull IntRange levelRange,
    @NotNull DoubleRange healthPerLevel,
    @NotNull DoubleRange damagePerLevel,
    @NotNull Map<EquipmentSlot, ItemStack> equipment,
    @NotNull Object2DoubleMap<ItemStack> drops,
    @NotNull List<String> abilities
) {

}

package io.github.minehollow.ranks.reward;


import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.range.IntRange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record RankReward(
  @NotNull String id,
  @NotNull IntRange range,
  int everyXLevels,
  @Nullable String permissionToReceive,
  @NotNull String displayName,
  @NotNull List<String> commandsToExecute,
  @NotNull List<ItemStack> itemsToGive
) {

    public static @NotNull RankReward readFromSection(
      @NotNull ConfigurationSection section
    ) {
        final var id = section.getName();
        final var range = IntRange.parseString(section.getString("range", "1"));
        final var everyXLevels = section.getInt("every-x-levels", 0);
        final var permissionToReceive = section.getString("permission-to-receive");
        final var displayName = requireNonNull(section.getString("display-name"), "Display name is required for reward: " + id);
        final var commands = section.getStringList("commands");
        final var rawItemsToGive = section.getList("items-to-give");

        @SuppressWarnings("unchecked")
        List<ItemStack> itemsToGive = rawItemsToGive == null ? (List<ItemStack>) rawItemsToGive : rawItemsToGive.stream()
          .filter(obj -> obj instanceof ItemStack)
          .map(obj -> (ItemStack) obj)
          .toList();

        return new RankReward(id, range, everyXLevels,
          permissionToReceive, displayName, commands, itemsToGive);
    }

    public void writeToSection(@NotNull ConfigurationSection rootSection) {
        final var section = rootSection.createSection(id);
        section.set("range", range.toString());
        section.set("every-x-levels", everyXLevels);
        section.set("permission-to-receive", permissionToReceive);
        section.set("display-name", displayName);
        section.set("commands", commandsToExecute);
        section.set("items-to-give", itemsToGive);
    }

    public boolean canBeGivenToPlayer(@NotNull Player player) {
        if (permissionToReceive != null && !player.hasPermission(permissionToReceive)) {
            return false;
        }

        if (itemsToGive.isEmpty()) {
            return true;
        }

        return itemsToGive.stream()
          .allMatch(item -> PlayerUtil.hasSpace(player, item, item.getAmount()));
    }
}

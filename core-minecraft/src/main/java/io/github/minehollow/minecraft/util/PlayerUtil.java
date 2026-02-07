/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package io.github.minehollow.minecraft.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PlayerUtil {

    public void removeItemInHand(@NonNull Player player, int amount) {
        if (amount <= 0) {
            return;
        }

        final ItemStack item = player.getItemInHand();
        if (item.getAmount() <= amount) {
            player.setItemInHand(null);
        } else {
            item.setAmount(item.getAmount() - amount);
        }
    }

    public int addItem(@NonNull Player player, @NonNull ItemStack itemStack, int amount) {
        final Inventory inventory = player.getInventory();

        if (amount <= itemStack.getMaxStackSize()) {
            itemStack.setAmount(amount);
            return inventory.addItem(itemStack).values()
                .stream().reduce(0, (a, b) -> a + b.getAmount(), Integer::sum);
        }

        int maxStack = itemStack.getMaxStackSize();

        int value = amount / maxStack;
        int rest = amount - maxStack * value;

        for (int index = 0; index < value; index++) {
            itemStack.setAmount(maxStack);

            Collection<ItemStack> item = inventory.addItem(itemStack).values();
            if (!item.isEmpty()) {
                return rest + (value - index) * maxStack;
            }
        }

        if (rest > 0) {
            itemStack.setAmount(rest);
            return inventory.addItem(itemStack).values()
                .stream().reduce(0, (a, b) -> a + b.getAmount(), Integer::sum);
        }

        return 0;
    }

    /**
     * Returns whether the player's inventory is empty.
     *
     * <p>Checks main inventory slots, armor slots, and the item on the cursor.
     *
     * @param player the player to check (must not be {@code null})
     * @return {@code true} if the main inventory, armor slots, and cursor contain no items;
     *         {@code false} otherwise
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public boolean isEmpty(@NonNull Player player) {
        final PlayerInventory inventory = player.getInventory();
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) return false;
        }

        for (ItemStack itemStack : inventory.getArmorContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) return false;
        }

        final ItemStack cursor = player.getItemOnCursor();
        return cursor.getType() == Material.AIR;
    }

    public static boolean hasSpace(@NotNull Player player, @NotNull ItemStack itemStack) {
        return hasSpace(player, itemStack, itemStack.getAmount());
    }

    /**
     * Checks if the player has space in the inventory.
     *
     * @param player The player.
     * @param itemStack The item stack.
     * @param amount The amount.
     *
     * @return True if the player has space, false otherwise.
     */
    public boolean hasSpace(@NotNull Player player, @NotNull ItemStack itemStack, int amount) {
        final PlayerInventory inventory = player.getInventory();
        int slots = (int) Math.ceil((double) amount / itemStack.getMaxStackSize());

        int count = 0;
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) count++;
            if (count >= slots) return true;
        }

        return false;
    }
}

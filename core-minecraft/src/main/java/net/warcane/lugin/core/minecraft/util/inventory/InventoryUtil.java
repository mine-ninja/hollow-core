package net.warcane.lugin.core.minecraft.util.inventory;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class InventoryUtil {

    /**
     * Returns all items of a given type present in a player inventory.
     *
     * @param player Player whose inventory will be checked.
     * @param type   The type of item that will be searched for in the inventory.
     * @return All found items of the specified type.
     */
    public ItemStack[] getByType(@NotNull Player player, Material type) {
        ItemStack[] itemArray = new ItemStack[0];
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.getType() == type) {
                itemArray = Arrays.copyOf(itemArray, itemArray.length + 1);
                itemArray[itemArray.length - 1] = content;
            }
        }

        return itemArray;
    }

    /**
     * Gets the quantity of an item in the player's inventory.
     *
     * @param player    Player whose inventory is checked
     * @param itemStack The item that will be checked
     * @return Number of items in inventory
     */
    public int getItemAmount(Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return 0;

        return Arrays.stream(player.getInventory().getContents())
            .filter(item -> item != null && item.isSimilar(itemStack))
            .mapToInt(ItemStack::getAmount)
            .sum();
    }

    /**
     * Gets the quantity of a material in the player's inventory.
     *
     * @param player   Player whose inventory is checked
     * @param material Type of item that will be checked
     * @return Number of materials in inventory
     */
    public int getMaterialAmount(@NotNull Player player, Material material) {
        return Arrays.stream(player.getInventory().getContents())
            .filter(item -> item != null && item.getType() == material)
            .mapToInt(ItemStack::getAmount)
            .sum();
    }

    /**
     * Gets the name of an item based on its meta or type.
     *
     * @param itemStack The item to get the name of.
     * @return The name of the item.
     */
    public String getItemName(@NotNull ItemStack itemStack) {
        return itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
            ? itemStack.getItemMeta().getDisplayName()
            : Arrays.stream(itemStack.getType().name().toLowerCase().split("_"))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    /**
     * Checks whether a player has enough inventory space to store a given amount of items.
     *
     * @param inventory     Whose inventory will be checked.
     * @param itemStacks A variable number of items that you want to check if they fit in the player's inventory.
     * @return Player has enough space for all items or not.
     */
    public boolean fits(@NotNull Inventory inventory, ItemStack... itemStacks) {
        int emptySlots = 0;

        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) emptySlots++;
        }

        return emptySlots >= itemStacks.length;
    }

    /**
     * Filter a set of items to return only those that are not null,
     *
     * @param itemStacks Items to be processed.
     * @return An array containing only items that are not null,
     */
    public ItemStack[] ignoreNullItems(ItemStack @NotNull ... itemStacks) {
        ItemStack[] itemArray = new ItemStack[0];
        for (ItemStack content : itemStacks) {
            if (content != null && content.getType() != Material.AIR) {
                itemArray = Arrays.copyOf(itemArray, itemArray.length + 1);
                itemArray[itemArray.length - 1] = content;
            }
        }

        return itemArray;
    }

    /**
     * Adds ItemStack to a player's inventory, ignoring invalid items.
     *
     * @param player     The player who will receive the items.
     * @param itemStacks The items that will be delivered to the player.
     */
    public void give(Player player, boolean canDrop, ItemStack @NotNull ... itemStacks) {
        for (ItemStack content : itemStacks) {
            if (content == null || content.getType() == Material.AIR) continue;

            if (player.getInventory().firstEmpty() == -1 && canDrop) {
                player.getWorld().dropItemNaturally(player.getLocation(), content);
                return;
            }

            player.getInventory().addItem(content);
        }
    }

    /**
     * Removes an item amount from the inventory.
     *
     * @param inventory The inventory from which the items will be removed.
     * @param itemStack The item that will be removed.
     * @param amount    The amount of items to be removed.
     */
    public void removeItemInInventory(@NotNull Inventory inventory, @NotNull ItemStack itemStack, int amount) {
        for (ItemStack content : inventory.getContents()) {
            if (content == null || content.getType() == Material.AIR) continue;
            if (content.isSimilar(itemStack)) {
                final int itemAmount = content.getAmount();

                if (itemAmount <= amount) {
                    amount -= itemAmount;
                    inventory.removeItem(content);
                } else {
                    content.setAmount(itemAmount - amount);
                    break;
                }

                if (amount <= 0) break;
            }
        }
    }

    /**
     * Removes a material amount from the inventory.
     *
     * @param inventory The inventory from which the items will be removed.
     * @param material The material that will be removed.
     * @param amount    The amount of items to be removed.
     */
    public void removeMaterialInInventory(@NotNull Inventory inventory, @NotNull Material material, int amount) {
        for (ItemStack content : inventory.getContents()) {
            if (content == null || content.getType() == Material.AIR) continue;
            if (content.getType() == material) {
                final int itemAmount = content.getAmount();

                if (itemAmount <= amount) {
                    amount -= itemAmount;
                    inventory.removeItem(content);
                } else {
                    content.setAmount(itemAmount - amount);
                    break;
                }

                if (amount <= 0) break;
            }
        }
    }

    /**
     * Returns the amount of empty slots in a inventory.
     *
     * @param inventory The inventory will be checked.
     * @return The number of empty slots in the inventory.
     */
    public int getFreeSlots(@NotNull PlayerInventory inventory) {
        int freeSlots = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    /**
     * Removes an item amount from the player's main hand.
     *
     * @param player The player who will have the items removed.
     */
    public void removeFromInMainHand(@NotNull Player player, int amount) {
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) return;

        if (itemInHand.getAmount() <= amount) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            itemInHand.setAmount(itemInHand.getAmount() - amount);
            player.getInventory().setItemInMainHand(itemInHand);
        }
    }

    /**
     * Removes an item amount from the player's offhand.
     *
     * @param player The player who will have the items removed.
     */
    public void removeFromInOffHand(@NotNull Player player, int amount) {
        final ItemStack itemInHand = player.getInventory().getItemInOffHand();
        if (itemInHand.getType() == Material.AIR) return;

        if (itemInHand.getAmount() <= amount) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        } else {
            itemInHand.setAmount(itemInHand.getAmount() - amount);
            player.getInventory().setItemInOffHand(itemInHand);
        }
    }
}

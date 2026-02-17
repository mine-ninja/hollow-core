package io.github.minehollow.mailbox.model;

import io.github.minehollow.minecraft.util.ItemSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailboxItem {
    private String id;
    private String senderPlugin;
    private String description;
    private List<String> serializedItems;
    private long sentAt;
    private String icon;

    public static MailboxItem create(
            @NotNull String senderPlugin,
            @NotNull String description,
            @NotNull ItemStack[] items,
            @NotNull String icon) {
        List<String> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                serialized.add(ItemSerializer.serialize(item));
            }
        }

        return new MailboxItem(
                UUID.randomUUID().toString(),
                senderPlugin,
                description,
                serialized,
                System.currentTimeMillis(),
                icon);
    }

    public static MailboxItem create(
            @NotNull String senderPlugin,
            @NotNull String description,
            @NotNull ItemStack[] items) {
        return create(senderPlugin, description, items, "CHEST");
    }

    @NotNull
    public ItemStack[] getItems() {
        if (serializedItems == null || serializedItems.isEmpty()) {
            return new ItemStack[0];
        }
        return serializedItems.stream()
                .map(ItemSerializer::deserialize)
                .filter(item -> item != null)
                .toArray(ItemStack[]::new);
    }

    public int getItemCount() {
        return serializedItems != null ? serializedItems.size() : 0;
    }
}

package io.github.minehollow.kits.kit.model;

import io.github.minehollow.minecraft.util.ItemSerializer;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Kit {

    @BsonId
    private String id;
    private String displayName;
    private long cooldown;

    @Setter(AccessLevel.NONE)
    private List<String> serializedItems;

    @BsonIgnore
    public List<ItemStack> getItems() {
        if (serializedItems == null) return List.of();
        return serializedItems.stream()
            .map(ItemSerializer::deserialize)
            .filter(Objects::nonNull)
            .toList();
    }

    public void setItems(List<ItemStack> items) {
        this.serializedItems = items.stream()
            .map(ItemSerializer::serialize)
            .filter(Objects::nonNull)
            .toList();
    }

    public boolean hasCooldown() {
        return cooldown > 0;
    }

    @BsonIgnore
    public String getPermission() {
        return "kit.%s".formatted(id);
    }
}

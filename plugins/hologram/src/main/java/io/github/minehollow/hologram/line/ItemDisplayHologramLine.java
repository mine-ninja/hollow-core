package io.github.minehollow.hologram.line;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A hologram line backed by an ITEM_DISPLAY entity.
 */
public class ItemDisplayHologramLine implements HologramLine {

    private Material material;
    private float scale;
    private WrapperEntity entity;

    public ItemDisplayHologramLine(@NotNull Material material) {
        this(material, 1.0f);
    }

    public ItemDisplayHologramLine(@NotNull Material material, float scale) {
        this.material = material;
        this.scale = scale;
    }

    @Override
    public @NotNull HologramLineType getType() {
        return HologramLineType.ITEM;
    }

    @Override
    public void spawn(@NotNull Location location) {
        entity = new WrapperEntity(EntityTypes.ITEM_DISPLAY);
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();

        ItemType itemType = SpigotConversionUtil.fromBukkitItemMaterial(material);
        if (itemType == null) {
            itemType = ItemTypes.STONE;
        }

        meta.setItem(new com.github.retrooper.packetevents.protocol.item.ItemStack.Builder()
            .type(itemType)
            .amount(1)
            .build());
        meta.setBillboardConstraints(ItemDisplayMeta.BillboardConstraints.CENTER);
        meta.setDisplayType(ItemDisplayMeta.DisplayType.FIXED);

        if (scale != 1.0f) {
            meta.setScale(new Vector3f(scale, scale, scale));
        }

        entity.spawn(SpigotConversionUtil.fromBukkitLocation(location));
    }

    @Override
    public void despawn() {
        if (entity != null) {
            entity.despawn();
            entity.remove();
            entity = null;
        }
    }

    @Override
    public void teleport(@NotNull Location location) {
        if (entity != null) {
            entity.teleport(SpigotConversionUtil.fromBukkitLocation(location));
        }
    }

    @Override
    public void addViewer(@NotNull UUID uuid) {
        if (entity != null) {
            entity.addViewer(uuid);
        }
    }

    @Override
    public void removeViewer(@NotNull UUID uuid) {
        if (entity != null) {
            entity.removeViewer(uuid);
        }
    }

    @Override
    public WrapperEntity getEntity() {
        return entity;
    }

    @Override
    public double getHeight() {
        return 0.5;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "ITEM");
        map.put("material", material.name());
        if (scale != 1.0f) {
            map.put("scale", scale);
        }
        return map;
    }

    public @NotNull Material getMaterial() {
        return material;
    }

    public void setMaterial(@NotNull Material material) {
        this.material = material;
        if (entity != null) {
            ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
            ItemType itemType = SpigotConversionUtil.fromBukkitItemMaterial(material);
            if (itemType == null) {
                itemType = ItemTypes.STONE;
            }
            meta.setItem(new com.github.retrooper.packetevents.protocol.item.ItemStack.Builder()
                .type(itemType)
                .amount(1)
                .build());
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        if (entity != null) {
            ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
            meta.setScale(new Vector3f(scale, scale, scale));
        }
    }
}

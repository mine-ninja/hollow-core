package io.github.minehollow.hologram.line;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.BlockDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A hologram line backed by a BLOCK_DISPLAY entity.
 */
public class BlockDisplayHologramLine implements HologramLine {

    private Material material;
    private float scale;
    private WrapperEntity entity;

    public BlockDisplayHologramLine(@NotNull Material material) {
        this(material, 0.5f);
    }

    public BlockDisplayHologramLine(@NotNull Material material, float scale) {
        this.material = material;
        this.scale = scale;
    }

    @Override
    public @NotNull HologramLineType getType() {
        return HologramLineType.BLOCK;
    }

    @Override
    public void spawn(@NotNull Location location) {
        entity = new WrapperEntity(EntityTypes.BLOCK_DISPLAY);
        BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();

        int blockStateId = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getGlobalId();
        meta.setBlockId(blockStateId);
        meta.setBillboardConstraints(BlockDisplayMeta.BillboardConstraints.CENTER);

        meta.setScale(new Vector3f(scale, scale, scale));

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
        map.put("type", "BLOCK");
        map.put("material", material.name());
        if (scale != 0.5f) map.put("scale", scale);
        return map;
    }

    // ── Getters & Setters ────────────────────────────────────

    public @NotNull Material getMaterial() {
        return material;
    }

    public void setMaterial(@NotNull Material material) {
        this.material = material;
        if (entity != null) {
            BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();
            int blockStateId = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getGlobalId();
            meta.setBlockId(blockStateId);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        if (entity != null) {
            BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();
            meta.setScale(new Vector3f(scale, scale, scale));
        }
    }
}

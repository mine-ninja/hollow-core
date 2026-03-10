package io.github.minehollow.hologram.line;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A hologram line backed by a TEXT_DISPLAY entity.
 */
public class TextDisplayHologramLine implements HologramLine {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private String text;
    private float scale;
    private int backgroundColor;
    private boolean shadow;
    private WrapperEntity entity;

    public TextDisplayHologramLine(@NotNull String text) {
        this(text, 1.0f, 0x0, true);
    }

    public TextDisplayHologramLine(@NotNull String text, float scale, int backgroundColor, boolean shadow) {
        this.text = text;
        this.scale = scale;
        this.backgroundColor = backgroundColor;
        this.shadow = shadow;
    }

    @Override
    public @NotNull HologramLineType getType() {
        return HologramLineType.TEXT;
    }

    @Override
    public void spawn(@NotNull Location location) {
        entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

        Component component = MINI_MESSAGE.deserialize(text);
        meta.setText(component);
        meta.setBillboardConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(shadow);
        meta.setBackgroundColor(backgroundColor);

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
        return 0.3;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "TEXT");
        map.put("text", text);
        if (scale != 1.0f) map.put("scale", scale);
        if (backgroundColor != 0x0) map.put("background-color", backgroundColor);
        if (!shadow) map.put("shadow", false);
        return map;
    }

    // ── Getters & Setters ────────────────────────────────────

    public @NotNull String getText() {
        return text;
    }

    public void setText(@NotNull String text) {
        this.text = text;
        if (entity != null) {
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setText(MINI_MESSAGE.deserialize(text));
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        if (entity != null) {
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setScale(new Vector3f(scale, scale, scale));
        }
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        if (entity != null) {
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setBackgroundColor(backgroundColor);
        }
    }

    public boolean isShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        if (entity != null) {
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setShadow(shadow);
        }
    }
}

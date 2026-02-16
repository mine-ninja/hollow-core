package io.github.minehollow.bestiary.custom;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import io.github.minehollow.bestiary.spawner.CustomMobSpawner;
import io.github.minehollow.bestiary.util.ProximityUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import lombok.Setter;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Getter
@Setter
public class CustomMobEntity {

    private static final long MAX_INACTIVE_TIME = 120; // 2 minutos

    private final @NotNull Entity bukkitEntity;
    private final int level;
    private final @Nullable CustomMobSpawner sourceSpawner;
    private final Stopwatch inactiveTracker = new Stopwatch();

    private @Nullable WrapperEntity nameTag;
    private DoubleRange damagePerLevel;
    private DoubleRange healthPerLevel;

    public CustomMobEntity(@NotNull Location location, @NotNull EntityType entityType, int level, @Nullable CustomMobSpawner sourceSpawner) {
        this.bukkitEntity = location.getWorld().spawnEntity(location, entityType);
        this.level = level;
        this.sourceSpawner = sourceSpawner;
        this.bukkitEntity.setPersistent(false);
    }

    public void updateMetadata(@NotNull String displayName) {
        if (!(bukkitEntity instanceof LivingEntity living)) return;

        double health = living.getHealth();
        final var maxHealthAttribute = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute == null ? 0 : maxHealthAttribute.getValue();

        this.setNameTagLines(
            "<yellow>" + displayName + " <gray>[Nv. " + level + "]",
            "<red>❤ " + (int) health + "/" + (int) maxHealth
        );
    }

    public void setNameTagVisible(boolean visible) {
        if (visible && nameTag == null) {
            this.nameTag = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
            this.nameTag.spawn(SpigotConversionUtil.fromBukkitLocation(bukkitEntity.getLocation()));
            this.setupNameTagMeta();
        } else if (!visible && nameTag != null) {
            this.nameTag.despawn();
            this.nameTag = null;
        }
    }

    private void setupNameTagMeta() {
        this.modifyNameTag(meta -> {
            meta.setBillboardConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
            meta.setBackgroundColor(0);
        });
    }

    public void setNameTagLines(@NotNull String... lines) {
        this.setNameTagText(StringUtils.multiText(lines));
    }

    public void setNameTagText(@NotNull Component text) {
        if (nameTag == null) setNameTagVisible(true);
        modifyNameTag(meta -> meta.setText(text));
    }

    public void syncNameTagForPlayer(@NotNull Player player) {
        if (nameTag == null) return;



        this.nameTag.addViewer(player.getUniqueId());
        var packet = new WrapperPlayServerSetPassengers(bukkitEntity.getEntityId(), new int[]{nameTag.getEntityId()});
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void modifyNameTag(Consumer<TextDisplayMeta> consumer) {
        if (this.nameTag != null) {
            this.nameTag.consumeEntityMeta(TextDisplayMeta.class, consumer);
        }
    }

    public void tickActive() {
        if (ProximityUtil.hasPlayersAround(bukkitEntity.getLocation(), 64)) {
            inactiveTracker.reset();
        }
    }

    public boolean isInactive() {
        return inactiveTracker.elapsedTimeInSeconds() >= MAX_INACTIVE_TIME;
    }

    public void remove() {
        if (nameTag != null) nameTag.despawn();
        bukkitEntity.remove();
    }
}

package io.github.minehollow.bestiary.monster;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.UUID;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MonsterHologram {

    private static final int BG_COLOR = 0x40000000;

    private final Entity trackedEntity;
    private WrapperEntity display;

    public MonsterHologram(@NotNull Entity trackedEntity) {
        this.trackedEntity = trackedEntity;
        spawnDisplay();
    }

    public void update(@NotNull String displayName, int level, double health, double maxHealth) {
        if (display == null || !display.isSpawned()) {
            spawnDisplay();
        }

        TextDisplayMeta meta = (TextDisplayMeta) display.getEntityMeta();
        meta.setText(buildText(displayName, level, health, maxHealth));
        display.refresh();
    }

    public void remove() {
        if (display != null && display.isSpawned()) {
            display.despawn();
        }
        display = null;
    }

    private void spawnDisplay() {
        display = new WrapperEntity(UUID.randomUUID(), EntityTypes.TEXT_DISPLAY);

        TextDisplayMeta meta = (TextDisplayMeta) display.getEntityMeta();
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(BG_COLOR);

        display.spawn(SpigotConversionUtil.fromBukkitLocation(trackedEntity.getLocation()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            display.addViewer(player.getUniqueId());
            sendMountPacket(player);
        }
    }

    public void addViewer(@NotNull Player player) {
        if (display != null && display.isSpawned()) {
            display.addViewer(player.getUniqueId());
            sendMountPacket(player);
        }
    }

    public void removeViewer(@NotNull Player player) {
        if (display != null && display.isSpawned()) {
            display.removeViewer(player.getUniqueId());
        }
    }

    public void sendMountPacket(@NotNull Player player) {
        if (display == null || !display.isSpawned()) {
            return;
        }

        // Passengers atuais da entidade Bukkit + nosso display
        int[] bukkit = trackedEntity.getPassengers()
            .stream().mapToInt(Entity::getEntityId).toArray();

        int[] passengers = new int[bukkit.length + 1];
        System.arraycopy(bukkit, 0, passengers, 0, bukkit.length);
        passengers[bukkit.length] = display.getEntityId();

        PacketEvents.getAPI()
            .getPlayerManager()
            .sendPacket(
                player, new WrapperPlayServerSetPassengers(
                    trackedEntity.getEntityId(), passengers
                )
            );
    }


    private static Component buildText(String displayName, int level, double health, double maxHealth) {
        double ratio = maxHealth > 0 ? health / maxHealth : 0;

        final String hpGradient = ratio > 0.5
                                  ? "<gradient:#4cff6e:#a8ffbc:#4cff6e>"   // verde — brilho suave no meio
                                  : ratio > 0.25
                                    ? "<gradient:#ffd84c:#fff0a0:#ffd84c>"   // amarelo — realce quente
                                    : "<gradient:#ff4c4c:#ff9a9a:#ff4c4c>";  // vermelho — brilho rosado

        final String nameGradient = "<gradient:#e8e8e8:#ffffff:#e8e8e8>";
        final String levelGradient = "<gradient:#c8960c:#ffd966:#c8960c>";

        Component nameLine = StringUtils.formatString(
            nameGradient + displayName + " " + levelGradient + "[Nível. " + level + "]"
        );

        Component hpLine = StringUtils.formatString(
            String.format(hpGradient + "❤ %.1f / %.1f", health, maxHealth)
        );

        return nameLine.append(Component.newline()).append(hpLine);
    }
}
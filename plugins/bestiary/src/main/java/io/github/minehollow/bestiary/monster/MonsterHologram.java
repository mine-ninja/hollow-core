package io.github.minehollow.bestiary.monster;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
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

    // String hpColor;
    //        if (ratio > 0.7) {
    //            hpColor = "<#5dc922>";
    //        } else if (ratio > 0.5) {
    //            hpColor = "<#ffc526>";
    //        } else if(ratio > 0.3) {
    //            hpColor = "<#ff6b26>";
    //        } else {
    //            hpColor = "<#fc1212>";
    //        }


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
        meta.setShadow(false);
        meta.setBackgroundColor(0);
        meta.setTranslation(new Vector3f(0.0f, 0.2f, 0.0f));

        display.spawn(SpigotConversionUtil.fromBukkitLocation(trackedEntity.getLocation()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            display.addViewer(player.getUniqueId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, getPassengersPacket());
        }
    }

    public WrapperPlayServerSetPassengers getPassengersPacket() {
        return new WrapperPlayServerSetPassengers(trackedEntity.getEntityId(), new int[]{display.getEntityId()});
    }

    public int getEntityId() {
        return display != null ? display.getEntityId() : -1;
    }


    private static final char HEALTH_SYMBOL = '❤';
    private static final double[] R_T = {0.7, 0.5, 0.3, 0.0};
    private static final String[] C_T = {"<#5dc922>", "<#ffc526>", "<#ff6b26>", "<#fc1212>"};
    private static final String LVL_G = "<gradient:#f0b01d:#edb22b:#f0b01d>";

    private static Component buildText(String displayName, int level, double health, double maxHealth) {
        if (maxHealth <= 0) {
            return Component.empty();
        }
        final double ratio = health / maxHealth;

        String hpColor = C_T[0];
        for (int i = 0; i < 4; i++) {
            if (ratio > R_T[i]) {
                hpColor = C_T[i];
                break;
            }
        }

        return Component.text()
            .append(io.github.minehollow.minecraft.util.message.StringUtils.formatString(displayName))
            .append(Component.space())
            .append(io.github.minehollow.minecraft.util.message.StringUtils.formatString(LVL_G + "[Lvl " + level + "]"))
            .append(Component.newline())
            .append(StringUtils.formatString(hpColor + HEALTH_SYMBOL + " %.1f/%.1f".formatted(health, maxHealth)))
            .build();
    }
}
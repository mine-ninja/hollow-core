package io.github.minehollow.bestiary.display;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class DamageIndicator {
    private static final long LIFETIME_MS = 1_200L;     // 1.2s
    private static final double SPREAD_RADIUS = 1.1;        // raio horizontal ao redor do mob
    private static final double Y_MIN = 0.5;        // altura mínima acima do topo
    private static final double Y_MAX = 1.2;        // altura máxima acima do topo


    record TemporaryEntity(WrapperEntity entity, long despawnTime) {
        public static TemporaryEntity create(WrapperEntity entity) {
            return new TemporaryEntity(entity, System.currentTimeMillis() + LIFETIME_MS);
        }

        public boolean removeIfExpired() {
            if (System.currentTimeMillis() >= despawnTime) {
                entity.despawn();
                return true;
            }
            return false;
        }
    }


    private static final Stopwatch CLEANUP_TIMER = new Stopwatch();
    private static final Set<TemporaryEntity> ACTIVE_HOLOGRAMS = ConcurrentHashMap.newKeySet();

    /**
     * Spawna um indicador de dano flutuando ao redor do topo do mob. Pode ser chamado de qualquer thread.
     */
    public static void spawn(@NotNull Entity mob, double damage, boolean critical) {
        double topY = mob.getBoundingBox().getMaxY();
        Location loc = spawnLocation(mob.getLocation(), topY);

        // Spawn precisa ser na thread principal
        WrapperEntity display = new WrapperEntity(UUID.randomUUID(), EntityTypes.TEXT_DISPLAY);

        TextDisplayMeta meta = (TextDisplayMeta) display.getEntityMeta();
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(0);
        meta.setText(buildText(damage, critical));
        meta.setScale(new Vector3f(1.5f, 1.5f, 1.5f));

        display.spawn(SpigotConversionUtil.fromBukkitLocation(loc));

        for (Player player : mob.getWorld().getPlayers()) {
            display.addViewer(player.getUniqueId());
        }

        TemporaryEntity temp = TemporaryEntity.create(display);
        ACTIVE_HOLOGRAMS.add(temp);
    }

    private static Location spawnLocation(Location base, double topY) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(Math.PI * 2);
        double dist = rng.nextDouble(SPREAD_RADIUS * 0.3, SPREAD_RADIUS);
        double yExtra = rng.nextDouble(Y_MIN, Y_MAX);

        return new Location(
            base.getWorld(),
            base.getX() + Math.cos(angle) * dist,
            topY + yExtra,
            base.getZ() + Math.sin(angle) * dist
        );
    }

    private static Component buildText(double damage, boolean critical) {
        String formatted = critical
                           ? String.format("✦ %.1f ✦", damage)
                           : String.format("%.1f", damage);

        TextColor color = critical ? TextColor.color(0xFF6A00) : NamedTextColor.RED;

        return Component.text(formatted, color);
    }


    public static void init(@NotNull Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(
            new Listener() {
                @EventHandler
                public void handleRemoval(AsyncServerTickEvent event) {
                    if (CLEANUP_TIMER.resetIfElapsedSeconds(1)) {
                        ACTIVE_HOLOGRAMS.removeIf(TemporaryEntity::removeIfExpired);
                    }
                }
            }, plugin
        );
    }

    public static void shutdown() {
        ACTIVE_HOLOGRAMS.removeIf(TemporaryEntity::removeIfExpired);
    }
}
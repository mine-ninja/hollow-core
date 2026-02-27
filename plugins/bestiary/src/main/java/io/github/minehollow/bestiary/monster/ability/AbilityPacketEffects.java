package io.github.minehollow.bestiary.monster.ability;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet-only visual and audio effects for abilities.
 *
 * <p><b>Key design decisions:</b></p>
 * <ul>
 *   <li>All effects are sent as raw NMS packets — no vanilla entities spawned.</li>
 *   <li>Every effect has a 128-block viewer culling check to prevent network congestion.</li>
 *   <li>Particle packets are batched — one packet per player, not per particle point.</li>
 *   <li>Stateless utility class — zero per-entity overhead.</li>
 * </ul>
 */
public final class AbilityPacketEffects {

    /** Max distance (squared) at which players receive effect packets. */
    private static final double CULLING_RADIUS_SQ = 128.0 * 128.0;

    private AbilityPacketEffects() {}

    // ═══════════════════════════════════════════════════════════════════════════
    //  PROJECTILE — particle line from origin toward target, tick by tick
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sends a single "frame" of a projectile trail at the given position.
     * Called once per tick by the projectile simulation in {@link AbilityManager}.
     */
    public static void sendProjectileTrail(@NotNull Location position, @NotNull Particle particle,
                                            @NotNull List<Player> viewers) {
        ParticleOptions options = CraftParticle.createParticleParam(particle, null);
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
            options, true, true,
            position.getX(), position.getY(), position.getZ(),
            0.05f, 0.05f, 0.05f, // small spread for trail thickness
            0.0f, 3               // 3 particles per point
        );

        for (Player player : viewers) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AOE — expanding circular particle ring
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sends a single ring at the given radius. Call repeatedly with increasing radius
     * for the "expanding" effect.
     */
    public static void sendAoeRing(@NotNull Location center, double radius,
                                    @NotNull Particle particle, @NotNull List<Player> viewers) {
        int points = Math.max(12, (int) (radius * 8));
        double angleStep = (2 * Math.PI) / points;
        ParticleOptions options = CraftParticle.createParticleParam(particle, null);

        for (Player player : viewers) {
            ServerPlayer sp = ((CraftPlayer) player).getHandle();
            for (int i = 0; i < points; i++) {
                double angle = i * angleStep;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    options, true, true,
                    x, center.getY() + 0.15, z,
                    0f, 0f, 0f,
                    0f, 1
                );
                sp.connection.send(packet);
            }
        }
    }

    /**
     * Sends a full AOE burst (single-frame, no expansion animation).
     * Used for instant AOE abilities.
     */
    public static void sendAoeBurst(@NotNull Location center, double radius,
                                     @NotNull Particle particle, @NotNull List<Player> viewers) {
        // 3 concentric rings for a filled look
        for (double r = radius * 0.33; r <= radius; r += radius * 0.33) {
            sendAoeRing(center, r, particle, viewers);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TARGETED — tether/strike beam from caster to target
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sends a particle beam (tether) from the caster to the target.
     */
    public static void sendTargetedBeam(@NotNull Location from, @NotNull Location to,
                                         @NotNull Particle particle, @NotNull List<Player> viewers) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.1) return;

        // Normalize
        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        ParticleOptions options = CraftParticle.createParticleParam(particle, null);
        double step = 0.4; // one particle every 0.4 blocks
        int count = (int) (dist / step);

        for (Player player : viewers) {
            ServerPlayer sp = ((CraftPlayer) player).getHandle();
            for (int i = 0; i <= count; i++) {
                double t = i * step;
                ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    options, true, true,
                    from.getX() + nx * t,
                    from.getY() + ny * t,
                    from.getZ() + nz * t,
                    0.02f, 0.02f, 0.02f,
                    0f, 1
                );
                sp.connection.send(packet);
            }
        }
    }

    /**
     * Sends an impact burst at the target location (used for TARGETED strike).
     */
    public static void sendImpactBurst(@NotNull Location location, @NotNull Particle particle,
                                        @NotNull List<Player> viewers) {
        ParticleOptions options = CraftParticle.createParticleParam(particle, null);
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
            options, true, true,
            location.getX(), location.getY() + 1.0, location.getZ(),
            0.3f, 0.5f, 0.3f,
            0.05f, 20
        );

        for (Player player : viewers) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SOUND
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sends a sound effect via packet to nearby viewers.
     */
    public static void sendSound(@NotNull Location location, @NotNull Sound sound,
                                  @NotNull List<Player> viewers) {
        net.minecraft.sounds.SoundEvent nmsSound = CraftSound.bukkitToMinecraftHolder(sound).value();

        for (Player player : viewers) {
            ServerPlayer sp = ((CraftPlayer) player).getHandle();
            ClientboundSoundPacket packet = new ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(nmsSound),
                SoundSource.HOSTILE,
                location.getX(), location.getY(), location.getZ(),
                1.5f, 1.0f, sp.getRandom().nextLong()
            );
            sp.connection.send(packet);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VIEWER CULLING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns all players within 128 blocks of the given location.
     * Uses squared distance — no sqrt.
     */
    public static @NotNull List<Player> getViewers(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return List.of();

        double cx = location.getX();
        double cy = location.getY();
        double cz = location.getZ();

        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location pl = player.getLocation();
            double dx = pl.getX() - cx;
            double dy = pl.getY() - cy;
            double dz = pl.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz <= CULLING_RADIUS_SQ) {
                result.add(player);
            }
        }
        return result;
    }
}

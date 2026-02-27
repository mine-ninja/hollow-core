package io.github.minehollow.bestiary.monster.ability;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Built-in TACKLE ability executor.
 *
 * <p><b>Behaviour:</b> the mob dashes toward the closest target over a short
 * burst (~10 ticks / 0.5 s). On each tick it teleports forward, leaving a
 * particle trail. When it reaches the target (or the dash expires), every
 * player within the ability's radius takes damage and gets knocked back.</p>
 *
 * <p>No tasks are created — the dash is submitted as a {@link AbilityManager.LiveAbility}
 * and processed in the existing tick loop via {@code AsyncServerTickEvent}.
 * All main-thread work (teleport, damage, velocity) is batched via
 * {@link AbilityManager#scheduleSyncWork(Runnable)}.</p>
 *
 * <p><b>Config example:</b></p>
 * <pre>
 * tackle:
 *   display-name: "&c⚔ Tackle"
 *   type: TACKLE
 *   damage: "6.0-12.0"
 *   cooldown: 4000
 *   range: 10.0
 *   radius: 2.5
 *   speed: 1.5
 *   particle: CRIT
 *   sound: ENTITY_RAVAGER_ATTACK
 * </pre>
 */
public final class TackleExecutor implements AbilityExecutor {

    private final AbilityManager abilityManager;

    public TackleExecutor(@NotNull AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @Override
    public void execute(@NotNull AbilityCastContext ctx) {
        Player target = ctx.closestTarget();
        if (target == null) return;

        LivingEntity caster = ctx.caster();
        AbilityDefinition ability = ctx.ability();

        // Snapshot target position at cast time
        Location targetLoc = target.getLocation().clone();
        Location startLoc = caster.getLocation().clone();

        // Direction from caster → target (horizontal only for a ground dash)
        Vector direction = targetLoc.toVector().subtract(startLoc.toVector());
        direction.setY(0);
        double totalDistance = direction.length();
        if (totalDistance < 0.5) return; // too close, skip
        direction.normalize();

        // Play cast sound
        ctx.playCastSound();

        // Freeze AI via sync work (setAware must be on main thread)
        boolean hadAI = caster instanceof Mob mob && mob.isAware();
        abilityManager.scheduleSyncWork(() -> {
            if (caster instanceof Mob mob) mob.setAware(false);
        });

        // Submit the dash as a LiveAbility — no task created
        abilityManager.addLiveAbility(new LiveTackle(abilityManager, caster, ability, direction, totalDistance, hadAI));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LiveTackle — tick-driven dash state, zero task overhead
    // ═════════════════════════════════════════════════════════════════════

    static final class LiveTackle implements AbilityManager.LiveAbility {

        /** Max dash duration in ticks. */
        private static final int MAX_TICKS = 10;
        /** Knockback strength multiplier. */
        private static final double KNOCKBACK_STRENGTH = 0.8;

        private final AbilityManager manager;
        private final LivingEntity caster;
        private final AbilityDefinition ability;
        private final Vector direction;
        private final Vector velocity;
        private final double totalDistance;
        private final Particle trailParticle;
        private final boolean hadAI;

        private double travelled;
        private int tickCount;
        private boolean finished;

        LiveTackle(@NotNull AbilityManager manager, @NotNull LivingEntity caster,
                   @NotNull AbilityDefinition ability, @NotNull Vector direction,
                   double totalDistance, boolean hadAI) {
            this.manager = manager;
            this.caster = caster;
            this.ability = ability;
            this.direction = direction;
            this.totalDistance = totalDistance;
            this.hadAI = hadAI;
            this.trailParticle = ability.getParticle() != null ? ability.getParticle() : Particle.CRIT;

            double speed = ability.getSpeed() > 0 ? ability.getSpeed() : 1.2;
            double distPerTick = Math.min(speed, totalDistance / MAX_TICKS);
            this.velocity = direction.clone().multiply(distPerTick);
        }

        @Override
        public boolean tick() {
            if (finished) return true;
            tickCount++;

            // Caster dead or removed — abort
            if (!caster.isValid() || caster.isDead()) {
                finish();
                return true;
            }

            // ── Move caster forward (teleport on main thread) ──
            Location current = caster.getLocation();
            Location destination = current.clone().add(velocity);
            destination.setDirection(direction);
            manager.scheduleSyncWork(() -> caster.teleport(destination));
            travelled += velocity.length();

            // Trail VFX (packet-based, thread-safe)
            List<Player> viewers = AbilityPacketEffects.getViewers(current);
            AbilityPacketEffects.sendProjectileTrail(
                destination.clone().add(0, 0.3, 0), trailParticle, viewers
            );

            // Check if dash is done
            if (travelled < totalDistance && tickCount < MAX_TICKS) {
                return false; // keep ticking
            }

            // ── Impact ──
            Location impactLoc = destination;
            List<Player> impactViewers = AbilityPacketEffects.getViewers(impactLoc);
            double damage = ability.getDamageRange().roll();
            double radiusSq = ability.getRadiusSquared();

            // AOE ring at impact (packet-based, thread-safe)
            AbilityPacketEffects.sendAoeRing(impactLoc, ability.getRadius(), trailParticle, impactViewers);

            // Damage + knockback all players within radius — batched into single sync flush
            for (Player hit : AbilityManager.findPlayersInRange(impactLoc, ability.getRadius())) {
                if (impactLoc.distanceSquared(hit.getLocation()) > radiusSq) continue;

                // Compute knockback direction (read-only, safe off main thread)
                Vector knockback = hit.getLocation().toVector().subtract(impactLoc.toVector());
                if (knockback.lengthSquared() < 0.01) knockback = new Vector(0, 0, 0.1);
                knockback.normalize().multiply(KNOCKBACK_STRENGTH).setY(0.35);

                AbilityPacketEffects.sendImpactBurst(hit.getLocation(), trailParticle, impactViewers);

                // Enqueue damage + velocity — all flushed in one sync task
                final Vector kb = knockback;
                manager.scheduleSyncWork(() -> {
                    hit.setVelocity(hit.getVelocity().add(kb));
                    hit.damage(damage, caster);
                });
            }

            finish();
            return true;
        }

        private void finish() {
            finished = true;
            // Restore AI on main thread
            manager.scheduleSyncWork(() -> {
                if (caster instanceof Mob mob && caster.isValid()) {
                    mob.setAware(hadAI);
                }
            });
        }
    }
}

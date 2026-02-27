package io.github.minehollow.bestiary.monster.ability;

import io.github.minehollow.bestiary.monster.ActiveMonster;
import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Central ability manager — handles ability execution, active projectile simulation,
 * and provides the public API for external AI controllers.
 *
 * <p><b>Architecture:</b></p>
 * <ul>
 *   <li>Each {@link AbilityType} is mapped to an {@link AbilityExecutor} via
 *       the {@link AbilityExecutorRegistry}.</li>
 *   <li>Built-in types (PROJECTILE, AOE, TARGETED, TACKLE) are registered automatically.</li>
 *   <li>Custom types can be added at runtime via {@link #executors()}.</li>
 *   <li>All executors receive an {@link AbilityCastContext} containing <b>multiple targets</b>
 *       (all valid players in range, sorted closest-first).</li>
 *   <li>All main-thread work (damage, teleport, velocity) is batched into a single
 *       {@code Tasks.runSync()} call per tick via {@link #scheduleSyncWork(Runnable)}.</li>
 *   <li>All VFX are sent as packets via {@link AbilityPacketEffects} (thread-safe).</li>
 * </ul>
 */
public final class AbilityManager {

    /** Active in-flight projectiles being ticked. Thread-safe for async reads. */
    private final Set<LiveProjectile> activeProjectiles = ConcurrentHashMap.newKeySet();

    /** Active tick-driven abilities (tackles, beams, etc.). Thread-safe. */
    private final Set<LiveAbility> activeAbilities = ConcurrentHashMap.newKeySet();

    /**
     * Batched main-thread work queue. Executors and live abilities enqueue damage/teleport/velocity
     * operations here instead of calling {@code Tasks.runSync()} individually.
     * Flushed once per tick in {@link #tick()}.
     */
    private final Queue<Runnable> syncWorkQueue = new ConcurrentLinkedQueue<>();

    private final Plugin plugin;
    private final AbilityExecutorRegistry registry;

    public AbilityManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.registry = new AbilityExecutorRegistry(plugin.getLogger());
        registerBuiltInExecutors();
    }

    /**
     * @return the executor registry — use this to register custom ability types.
     */
    public @NotNull AbilityExecutorRegistry executors() {
        return registry;
    }

    /**
     * Enqueue a runnable to be executed on the main thread during the next tick flush.
     * Use this instead of {@code Tasks.runSync()} to avoid creating a new scheduled task
     * per operation. All queued work is flushed in a single sync task per tick.
     *
     * @param work the runnable to execute on the main thread
     */
    public void scheduleSyncWork(@NotNull Runnable work) {
        syncWorkQueue.add(work);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BUILT-IN EXECUTOR REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void registerBuiltInExecutors() {
        registry.register(AbilityType.PROJECTILE, this::executeProjectile);
        registry.register(AbilityType.AOE, this::executeAoe);
        registry.register(AbilityType.TARGETED, this::executeTargeted);
        registry.register(AbilityType.TACKLE, new TackleExecutor(this));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API — called by AbilityCastGoal or external AI controllers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cast an ability from the given monster.
     * Builds an {@link AbilityCastContext} with all valid targets in range,
     * then delegates to the registered {@link AbilityExecutor}.
     *
     * <p>Must be called from the main thread.</p>
     *
     * @param monster the active monster casting
     * @param index   the ability index in the model's ability list
     */
    public void cast(@NotNull ActiveMonster monster, int index) {
        List<AbilityDefinition> abilities = monster.model().getAbilities();
        if (index < 0 || index >= abilities.size()) return;

        AbilityDefinition ability = abilities.get(index);
        LivingEntity caster = monster.entity();

        AbilityExecutor executor = registry.get(ability.getType());
        if (executor == null) {
            plugin.getLogger().warning("[Abilities] No executor registered for type: " + ability.getType().name());
            return;
        }

        Location origin = caster.getEyeLocation();
        List<Player> targets = findPlayersInRange(origin, ability.getRange());
        List<Player> viewers = AbilityPacketEffects.getViewers(origin);

        AbilityCastContext ctx = new AbilityCastContext(
            monster, ability, caster, targets, origin.clone(), viewers
        );

        executor.execute(ctx);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BUILT-IN: PROJECTILE
    // ═══════════════════════════════════════════════════════════════════════════

    private void executeProjectile(@NotNull AbilityCastContext ctx) {
        Player target = ctx.closestTarget();
        if (target == null) return;

        AbilityDefinition ability = ctx.ability();
        Location origin = ctx.origin();

        Vector direction = target.getEyeLocation().toVector()
            .subtract(origin.toVector()).normalize();

        ctx.playCastSound();

        LiveProjectile proj = new LiveProjectile(
            this, ctx.caster(), origin.clone(), direction,
            ability.getSpeed(), ability.getRange(),
            ability.getDamageRange(), ability.getParticle()
        );
        activeProjectiles.add(proj);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BUILT-IN: AOE — instant radial damage + expanding ring via LiveAbility
    // ═══════════════════════════════════════════════════════════════════════════

    private void executeAoe(@NotNull AbilityCastContext ctx) {
        AbilityDefinition ability = ctx.ability();
        Location center = ctx.caster().getLocation();
        double radiusSq = ability.getRadiusSquared();
        double damage = ctx.rollDamage();
        LivingEntity caster = ctx.caster();

        // Batch all damage into a single sync flush
        for (Player player : ctx.targets()) {
            if (center.distanceSquared(player.getLocation()) > radiusSq) continue;
            scheduleSyncWork(() -> player.damage(damage, caster));
        }

        // VFX — expanding ring via tick-driven LiveAbility (no delayed tasks)
        if (ability.getParticle() != null) {
            activeAbilities.add(new LiveAoeRing(center.clone(), ability.getRadius(), ability.getParticle()));
        }

        ctx.playCastSound();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BUILT-IN: TARGETED — unavoidable lock-on with tether beam (all targets)
    // ═══════════════════════════════════════════════════════════════════════════

    private void executeTargeted(@NotNull AbilityCastContext ctx) {
        if (!ctx.hasTargets()) return;

        AbilityDefinition ability = ctx.ability();
        double damage = ctx.rollDamage();
        LivingEntity caster = ctx.caster();

        // Beam VFX (packet-based, thread-safe) + batch damage
        for (Player target : ctx.targets()) {
            if (ability.getParticle() != null) {
                AbilityPacketEffects.sendTargetedBeam(
                    caster.getEyeLocation(),
                    target.getEyeLocation(),
                    ability.getParticle(),
                    ctx.viewers()
                );
                AbilityPacketEffects.sendImpactBurst(target.getLocation(), ability.getParticle(), ctx.viewers());
            }
            scheduleSyncWork(() -> target.damage(damage, caster));
        }

        ctx.playCastSound();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TICK — called every server tick via AsyncServerTickEvent
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tick all active projectiles and live abilities, then flush all batched
     * main-thread work in a single {@code Tasks.runSync()} call.
     *
     * <p>Must be called every server tick.</p>
     */
    public void tick() {
        if (!activeProjectiles.isEmpty()) {
            activeProjectiles.removeIf(LiveProjectile::tick);
        }

        if (!activeAbilities.isEmpty()) {
            activeAbilities.removeIf(LiveAbility::tick);
        }

        flushSyncWork();
    }

    /**
     * Flush all queued main-thread work in a single sync task.
     * If the queue is empty, no task is submitted.
     */
    private void flushSyncWork() {
        if (syncWorkQueue.isEmpty()) return;

        Tasks.runSync(() -> {
            Runnable work;
            while ((work = syncWorkQueue.poll()) != null) {
                work.run();
            }
        });
    }

    /**
     * Cleanup — remove all active projectiles, abilities, and pending work.
     */
    public void shutdown() {
        activeProjectiles.clear();
        activeAbilities.clear();
        syncWorkQueue.clear();
    }

    /**
     * @return number of active projectiles (for monitoring)
     */
    public int activeProjectileCount() {
        return activeProjectiles.size();
    }

    /**
     * Add a live projectile to be ticked by this manager.
     * Useful for custom ability executors that create projectiles.
     */
    public void addProjectile(@NotNull LiveProjectile projectile) {
        activeProjectiles.add(projectile);
    }

    /**
     * Add a tick-driven ability to be processed every server tick.
     * The ability will be removed automatically when {@link LiveAbility#tick()} returns true.
     *
     * <p>Use this for multi-tick abilities like tackles, beams, channels, etc.</p>
     */
    public void addLiveAbility(@NotNull LiveAbility ability) {
        activeAbilities.add(ability);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIVE ABILITY — interface for tick-driven ability state
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * A tick-driven ability instance managed by the {@link AbilityManager}.
     *
     * <p>Implementations hold mutable state for a single ability cast
     * (e.g. a tackle dash, a channelled beam, a lingering zone).
     * The manager calls {@link #tick()} every server tick and removes
     * the instance when it returns {@code true}.</p>
     *
     * <p>This avoids creating a new scheduled task per cast — all live
     * abilities are processed in a single loop during {@link AbilityManager#tick()}.</p>
     */
    public interface LiveAbility {

        /**
         * Advance this ability by one tick.
         *
         * @return {@code true} if the ability is finished and should be removed
         */
        boolean tick();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIVE AOE RING — expanding ring VFX driven by tick loop (no delayed tasks)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tick-driven expanding AOE ring VFX. Replaces 3 separate
     * {@code Tasks.runSyncLater} calls with a single LiveAbility.
     */
    static final class LiveAoeRing implements LiveAbility {

        private static final int TOTAL_FRAMES = 3;
        private static final int TICKS_PER_FRAME = 2;

        private final Location center;
        private final double maxRadius;
        private final org.bukkit.Particle particle;
        private int tickCount;

        LiveAoeRing(@NotNull Location center, double maxRadius, @NotNull org.bukkit.Particle particle) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.particle = particle;
        }

        @Override
        public boolean tick() {
            tickCount++;

            // Emit a ring every TICKS_PER_FRAME ticks
            if (tickCount % TICKS_PER_FRAME != 0) return false;

            int frame = tickCount / TICKS_PER_FRAME;
            if (frame > TOTAL_FRAMES) return true;

            double r = maxRadius * ((double) frame / TOTAL_FRAMES);
            AbilityPacketEffects.sendAoeRing(center, r, particle, AbilityPacketEffects.getViewers(center));

            return frame >= TOTAL_FRAMES;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TARGETING — public utilities for executor implementations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find all survival-mode, alive players within range of the origin.
     * Sorted by distance (closest first). Uses squared distance — no sqrt in the filter.
     *
     * <p>This is a public utility so custom {@link AbilityExecutor} implementations
     * can re-use the same targeting logic.</p>
     *
     * @param origin the center point
     * @param range  maximum distance
     * @return mutable list of players sorted closest-first (may be empty)
     */
    public static @NotNull List<Player> findPlayersInRange(@NotNull Location origin, double range) {
        double rangeSq = range * range;
        List<Player> result = new ArrayList<>();

        for (Player player : origin.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (player.isDead()) continue;

            double distSq = origin.distanceSquared(player.getLocation());
            if (distSq <= rangeSq) {
                result.add(player);
            }
        }

        // Sort by distance (closest first)
        result.sort(Comparator.comparingDouble(p -> origin.distanceSquared(p.getLocation())));
        return result;
    }

    /**
     * Find the nearest survival-mode player within range of the origin.
     * Uses squared distance — no sqrt.
     *
     * @return the nearest player, or null if none in range
     */
    public static Player findNearestPlayer(@NotNull Location origin, double range) {
        List<Player> players = findPlayersInRange(origin, range);
        return players.isEmpty() ? null : players.get(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIVE PROJECTILE — inner state for in-flight projectiles
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Represents an in-flight packet-based projectile.
     * <p>No entity is spawned — only particle packets are sent each tick.
     * Collision is detected by checking player bounding boxes along the path.</p>
     *
     * <p>Public so custom executors can create and submit projectiles
     * via {@link AbilityManager#addProjectile(LiveProjectile)}.</p>
     */
    public static final class LiveProjectile {
        private final AbilityManager manager;
        private final LivingEntity caster;
        private final Location position;
        private final Vector direction;
        private final double speed;
        private final double maxDistanceSq;
        private final DamageRange damageRange;
        private final org.bukkit.Particle particle;
        private final Location origin;

        public LiveProjectile(@NotNull AbilityManager manager,
                       @NotNull LivingEntity caster, @NotNull Location origin,
                       @NotNull Vector direction, double speed, double maxDistance,
                       @NotNull DamageRange damageRange, @org.jetbrains.annotations.Nullable org.bukkit.Particle particle) {
            this.manager = manager;
            this.caster = caster;
            this.origin = origin.clone();
            this.position = origin.clone();
            this.direction = direction.normalize();
            this.speed = speed;
            this.maxDistanceSq = maxDistance * maxDistance;
            this.damageRange = damageRange;
            this.particle = particle;
        }

        /**
         * Advance the projectile by one tick.
         *
         * @return true if the projectile should be removed (hit or expired)
         */
        boolean tick() {
            if (caster.isDead() || !caster.isValid()) return true;

            double dx = direction.getX() * speed;
            double dy = direction.getY() * speed;
            double dz = direction.getZ() * speed;

            position.add(dx, dy, dz);
            double travelledSq = origin.distanceSquared(position);

            if (travelledSq > maxDistanceSq) return true;
            if (position.getWorld() == null) return true;

            // VFX — trail particles (packet-based, thread-safe)
            if (particle != null) {
                List<Player> viewers = AbilityPacketEffects.getViewers(position);
                AbilityPacketEffects.sendProjectileTrail(position, particle, viewers);
            }

            // Collision detection against player bounding boxes
            double hitRadius = 0.5;
            for (Player player : position.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
                if (player.isDead()) continue;

                BoundingBox box = player.getBoundingBox().expand(hitRadius);
                if (box.contains(position.getX(), position.getY(), position.getZ())) {
                    double damage = damageRange.roll();

                    if (particle != null) {
                        List<Player> viewers = AbilityPacketEffects.getViewers(position);
                        AbilityPacketEffects.sendImpactBurst(position, particle, viewers);
                    }

                    // Enqueue damage — flushed in batch
                    manager.scheduleSyncWork(() -> player.damage(damage, caster));
                    return true;
                }
            }

            return position.getBlock().getType().isSolid();
        }
    }
}


package io.github.minehollow.bestiary.monster.goal;

import io.github.minehollow.bestiary.monster.ActiveMonster;
import io.github.minehollow.bestiary.monster.ability.AbilityCastGoal;
import io.github.minehollow.bestiary.monster.ability.AbilityManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.logging.Logger;

/**
 * Applies NMS pathfinder goals to custom monsters based on their {@link MobBehavior}.
 *
 * <p><b>Optimization notes:</b></p>
 * <ul>
 *   <li>All vanilla goals are stripped first — no wasted ticking on unused AI.</li>
 *   <li>Goals are only added if explicitly enabled in the behavior profile.</li>
 *   <li>Uses fixed priority ordering so goal selector resolves quickly.</li>
 *   <li>No reflection — uses Paper's Mojang-mapped NMS directly.</li>
 *   <li>Stateless utility class — zero per-entity overhead.</li>
 * </ul>
 */
public final class MobGoalManager {

    private static final Logger LOGGER = Logger.getLogger("Bestiary-Goals");

    // Goal priorities (lower = higher priority)
    private static final int PRIORITY_FLOAT          = 0;
    private static final int PRIORITY_ABILITY_CAST   = 1;
    private static final int PRIORITY_MELEE_ATTACK   = 2;
    private static final int PRIORITY_RANDOM_STROLL  = 5;
    private static final int PRIORITY_LOOK_AT_PLAYER = 6;
    private static final int PRIORITY_LOOK_AROUND    = 7;

    // Target priorities
    private static final int PRIORITY_HURT_BY        = 1;
    private static final int PRIORITY_NEAREST_PLAYER = 2;

    private MobGoalManager() {
        // Utility class — no instantiation
    }

    /**
     * Strips all vanilla goals and applies goals based on the given behavior.
     * Safe to call on non-Mob entities (will just skip).
     *
     * @param entity   the Bukkit living entity to configure
     * @param behavior the behavior profile
     */
    public static void applyBehavior(@NotNull LivingEntity entity, @NotNull MobBehavior behavior) {
        applyBehavior(entity, behavior, null, null);
    }

    /**
     * Strips all vanilla goals and applies goals + ability casting.
     *
     * @param entity         the Bukkit living entity to configure
     * @param behavior       the behavior profile
     * @param activeMonster  the ActiveMonster instance (nullable — no abilities if null)
     * @param abilityManager the AbilityManager for ability execution (nullable)
     */
    public static void applyBehavior(@NotNull LivingEntity entity, @NotNull MobBehavior behavior,
                                      @Nullable ActiveMonster activeMonster,
                                      @Nullable AbilityManager abilityManager) {
        net.minecraft.world.entity.LivingEntity nmsEntity = ((CraftLivingEntity) entity).getHandle();

        if (!(nmsEntity instanceof Mob mob)) {
            return; // Slimes, etc. that aren't Mob subclass
        }

        // ── Step 1: Nuke all existing goals for a clean slate ──
        clearAllGoals(mob);

        // ── Step 2: Inject only the goals this behavior requests ──
        EnumSet<MobGoalType> enabled = behavior.enabledGoals();

        GoalSelector goalSelector = mob.goalSelector;
        GoalSelector targetSelector = mob.targetSelector;

        // Float — prevents drowning, extremely cheap
        if (enabled.contains(MobGoalType.FLOAT_ON_WATER)) {
            goalSelector.addGoal(PRIORITY_FLOAT, new FloatGoal(mob));
        }

        // Melee attack — only if the entity is a PathfinderMob
        if (enabled.contains(MobGoalType.MELEE_ATTACK) && mob instanceof PathfinderMob pathfinder) {
            goalSelector.addGoal(PRIORITY_MELEE_ATTACK,
                new MeleeAttackGoal(pathfinder, behavior.getAttackSpeed(), true));
        }

        // Wander
        if (enabled.contains(MobGoalType.RANDOM_STROLL) && mob instanceof PathfinderMob pathfinder) {
            goalSelector.addGoal(PRIORITY_RANDOM_STROLL,
                new WaterAvoidingRandomStrollGoal(pathfinder, 1.0));
        }

        // Look at player
        if (enabled.contains(MobGoalType.LOOK_AT_PLAYER)) {
            goalSelector.addGoal(PRIORITY_LOOK_AT_PLAYER,
                new LookAtPlayerGoal(mob, Player.class, 8.0F));
        }

        // Random look around
        if (enabled.contains(MobGoalType.RANDOM_LOOK_AROUND)) {
            goalSelector.addGoal(PRIORITY_LOOK_AROUND, new RandomLookAroundGoal(mob));
        }

        // ── Target goals ──

        // Hurt-by retaliation
        if (enabled.contains(MobGoalType.HURT_BY_TARGET) && mob instanceof PathfinderMob pathfinder) {
            targetSelector.addGoal(PRIORITY_HURT_BY, new HurtByTargetGoal(pathfinder));
        }

        // Nearest player targeting
        if (enabled.contains(MobGoalType.TARGET_NEAREST_PLAYER)) {
            double range = behavior.getTargetRange();
            targetSelector.addGoal(PRIORITY_NEAREST_PLAYER,
                new NearestAttackableTargetGoal<>(mob, Player.class, true) {
                    @Override
                    protected double getFollowDistance() {
                        return range;
                    }
                });
        }

        // ── Ability casting goal ──
        if (activeMonster != null && abilityManager != null) {
            var abilities = activeMonster.model().getAbilities();
            if (abilities != null && !abilities.isEmpty()) {
                goalSelector.addGoal(PRIORITY_ABILITY_CAST,
                    new AbilityCastGoal(mob, activeMonster, abilityManager));
            }
        }
    }

    /**
     * Completely removes all goals and target goals from a mob.
     * This is the most efficient way — avoids iterating a copy list.
     */
    private static void clearAllGoals(@NotNull Mob mob) {
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
    }

    /**
     * Makes a mob completely passive — removes all targeting and attack goals.
     * Keeps movement/look goals intact.
     */
    public static void makePassive(@NotNull LivingEntity entity) {
        applyBehavior(entity, MobBehavior.DEFAULT_PASSIVE);
    }

    /**
     * Makes a mob aggressive with default settings.
     */
    public static void makeAggressive(@NotNull LivingEntity entity) {
        applyBehavior(entity, MobBehavior.DEFAULT_AGGRESSIVE);
    }
}


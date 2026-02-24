package io.github.minehollow.bestiary.monster;

import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.util.ProximityUtil;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.LivingEntity;

public final class ActiveMonster {

    private final LivingEntity entity;
    private final MonsterStats stats;
    private final MonsterHologram hologram;
    private final CustomMonsterModel model;

    private final Stopwatch inactiveTicks = new Stopwatch();

    public ActiveMonster(
        LivingEntity entity,
        MonsterStats stats,
        MonsterHologram hologram,
        CustomMonsterModel model
    ) {
        this.entity = entity;
        this.stats = stats;
        this.hologram = hologram;
        this.model = model;

    }


    public boolean hasPlayersAround(double radius) {
        final var loc = entity.getLocation();
        final var world = loc.getWorld();
        final int blockX = loc.getBlockX();
        final int blockY = loc.getBlockY();
        final int blockZ = loc.getBlockZ();

        return world != null && ProximityUtil.countOnlinePlayersAround(world, blockX, blockY, blockZ, radius) > 0;
    }

    public boolean isInactiveFor(long quantity, TimeUnit unit) {
        return inactiveTicks.hasElapsedSeconds(unit.toSeconds(quantity));
    }

    public void updateActivity() {
        inactiveTicks.reset();
    }

    public LivingEntity entity() {
        return entity;
    }

    public MonsterStats stats() {
        return stats;
    }

    public MonsterHologram hologram() {
        return hologram;
    }

    public CustomMonsterModel model() {
        return model;
    }
}
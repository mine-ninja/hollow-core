package io.github.minehollow.bestiary.spawner;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.archetype.MobArchetype;
import io.github.minehollow.bestiary.util.ProximityUtil;
import io.github.minehollow.minecraft.util.range.IntRange;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomMobSpawner {

    private static final int RANGE = Bukkit.getViewDistance() * 16;

    private UUID uniqueId;
    private Location location;
    private String archetypeId;
    private IntRange spawnAmount;
    private int spawnTime;

    private final Stopwatch spawnerTimer = new Stopwatch();

    public void performSpawn(BestiaryPlugin plugin) {
        if (!canPerformSpawn()) return;

        MobArchetype archetype = plugin.getMobArchetypeService().getById(this.archetypeId);
        if (archetype == null) return;

        int amount = this.spawnAmount.getRandomValue();
        for (int i = 0; i < amount; i++) {
            plugin.getCustomMobManager().spawnEntity(archetype, this.location, this);
        }

        this.spawnerTimer.reset();
    }

    public boolean hasNearbyPlayers() {
        return ProximityUtil.hasPlayersAround(location, RANGE);
    }

    public boolean canPerformSpawn() {
        return spawnerTimer.hasElapsedSeconds(spawnTime) && hasNearbyPlayers();
    }
}

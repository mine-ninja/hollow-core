package io.github.minehollow.bestiary.spawner;

import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Data
public class MonsterSpawner {

    private final UUID uniqueId;
    private Location location;

    private String monsterModelId;

    private double spawnIntervalSeconds;

    private double activationRadius;

    private int maxAlive;

    private int spawnCount;
    private final Stopwatch spawnTimer = new Stopwatch();

    public MonsterSpawner(
        @NotNull UUID uniqueId,
        @NotNull Location location,
        @NotNull String monsterModelId,
        double spawnIntervalSeconds,
        double activationRadius,
        int maxAlive,
        int spawnCount
    ) {
        this.uniqueId = uniqueId;
        this.location = location;
        this.monsterModelId = monsterModelId;
        this.spawnIntervalSeconds = spawnIntervalSeconds;
        this.activationRadius = activationRadius;
        this.maxAlive = maxAlive;
        this.spawnCount = spawnCount;
    }

    public boolean pollSpawnReady() {
        return spawnTimer.resetIfElapsedSeconds(spawnIntervalSeconds);
    }

    public void resetTimer() {
        spawnTimer.reset();
    }
}

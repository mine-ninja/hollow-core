package io.github.minehollow.mines.listener;

import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.util.stopwatch.Stopwatch;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.filler.MineFiller;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public record MineResetTaskListener(@NotNull MinesPlugin plugin) implements Listener {

    private static final Stopwatch TIMER = new Stopwatch();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void tick(AsyncServerTickEvent event) {
        if (!TIMER.resetIfElapsedSeconds(60)) {
            return;
        }


        final var mineManager = plugin.getMineManager();
        final var minesWorld = Bukkit.getWorld(mineManager.getMineWorldName());
        if (minesWorld == null) return;

        for (final var mine : mineManager.getMines().values()) {
            if (mine.isIdle() || mine.canReset()) {
                MineFiller.fillMine(mineManager, mine, null);
            }
        }
    }
}

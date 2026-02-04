package io.github.minehollow.mines.listener;

import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.filler.MineFiller;
import io.github.minehollow.mines.model.Mine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class MineFillerTaskListener implements Listener {

    private final MinesPlugin plugin;

    public MineFillerTaskListener(@NotNull MinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(AsyncServerTickEvent event) {
        // Marca o início do processamento de todas as minas
        long totalStart = System.nanoTime();
        int minesReset = 0;

        for (final Mine mine : plugin.getMineManager().getMines().values()) {
            if (mine.canReset()) {
                long mineStart = System.nanoTime();

                MineFiller.fillMine(plugin.getMineManager(), mine , null); //

                long mineEnd = System.nanoTime();
                debug("Mina '" + mine.getId() + "' resetada em " + (mineEnd - mineStart) + " ns.");
                minesReset++;
            }
        }

        if (minesReset > 0) {
            long totalEnd = System.nanoTime();
            debug("Total de " + minesReset + " minas processadas em " + (totalEnd - totalStart) + " ns.");
        }
    }

    private static void debug(@NotNull String message) {
        System.out.println("[MineFillerTaskListener] " + message);
    }
}
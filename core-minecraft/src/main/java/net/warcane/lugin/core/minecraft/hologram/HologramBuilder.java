package net.warcane.lugin.core.minecraft.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public class HologramBuilder {

    private final Location location;
    private final List<Function<Player, String>> lines = new ArrayList<>();

    private Predicate<Player> renderFilter = player -> true;
    private boolean autoUpdate = false;
    private long updateInterval = 20L;

    public HologramBuilder(Location location) {
        this.location = location;
    }

    public HologramBuilder withRenderFilter(Predicate<Player> renderFilter) {
        this.renderFilter = renderFilter;
        return this;
    }

    public HologramBuilder withLine(@NotNull Function<Player, String> lineFunction) {
        this.lines.add(lineFunction);
        return this;
    }

    public HologramBuilder withLine(String lineText) {
        return withLine(player -> lineText);
    }

    public HologramBuilder withAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public HologramBuilder withUpdateInterval(long updateInterval, TimeUnit unit) {
        this.updateInterval = unit.toSeconds(updateInterval) * 1000L;
        return this;
    }

    public Hologram build(HologramManager manager) {
        Hologram hologram = manager.createHologram(location);
        hologram.setRenderFilter(renderFilter);
        hologram.setAutoUpdate(autoUpdate);
        hologram.setUpdateInterval(updateInterval);

        for (Function<Player, String> lineFunction : lines) {
            hologram.addLine(lineFunction);
        }
        return hologram;
    }
}

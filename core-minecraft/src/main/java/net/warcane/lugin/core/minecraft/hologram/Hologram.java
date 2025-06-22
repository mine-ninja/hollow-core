package net.warcane.lugin.core.minecraft.hologram;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
@Setter
public class Hologram {

    private static final double LINE_SPACING = 0.30;
    protected static double DEFAULT_RANGE = NumberConversions.square(Bukkit.getViewDistance() << 4);


    private final UUID uniqueId;
    private Location location;

    private final List<HologramLine> lines = new ArrayList<>();

    private Predicate<Player> renderFilter = player -> true;
    private final Set<UUID> viewers = new HashSet<>();

    private boolean autoUpdate;
    private long updateInterval = 20L;

    public Hologram(@NotNull Location location) {
        this.uniqueId = UUID.randomUUID();
        this.location = location;
    }

    public boolean isInChunk(int chunkX, int chunkZ) {
        int hologramChunkX = location.getBlockX() >> 4;
        int hologramChunkZ = location.getBlockZ() >> 4;
        return hologramChunkX == chunkX && hologramChunkZ == chunkZ;
    }

    public boolean isShown(@NotNull Player player) {
        return viewers.contains(player.getUniqueId());
    }

    public boolean canView(@NotNull Player player) {
        return renderFilter.test(player)
               && player.getWorld().getName().equals(location.getWorld().getName())
               && player.getLocation().distanceSquared(location) <= DEFAULT_RANGE;
    }

    public void addLine(@NotNull String lineText) {
        addLine(player -> lineText);
    }


    public void addLine(@NotNull Function<Player, String> lineFunction) {
        var yOffset = lines.size() * LINE_SPACING;
        var lineLocation = location.clone().subtract(0, yOffset, 0);

        var line = new HologramLine(this, lineLocation, lineFunction);
        lines.add(line);

        location.getWorld()
          .getPlayers()
          .stream()
          .filter(this::canView)
          .forEach(line::showTo);
    }

    public void updateAllLines(@NotNull Player player) {
        for (HologramLine line : lines) {
            line.updateLine(player);
        }
    }

    public void updateAllLines() {
        forEachViewers(this::updateAllLines);
    }

    public void updateLineForPlayer(int lineIndex, @NotNull Player player) {
        if (lineIndex < 0 || lineIndex >= lines.size()) return; // Invalid index

        HologramLine line = lines.get(lineIndex);
        line.updateLine(player);
    }

    public void destroyLine(int lineIndex) {
        var lineToRemove = getLine(lineIndex);
        if (lineToRemove != null) {
            lines.remove(lineIndex);
            forEachViewers(lineToRemove::hideTo);
        }
    }


    @Nullable
    public HologramLine getLine(int index) {
        try {
            return lines.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    public void teleport(@NotNull Location newLocation) {
        this.location = newLocation;

        for (HologramLine line : lines) {
            double yOffset = newLocation.getY() - (lines.indexOf(line) * LINE_SPACING);
            line.setLocation(newLocation.clone().add(0, yOffset, 0));
            forEachViewers(line::teleport);
        }
    }

    public void showToAll() {
        location.getWorld()
          .getPlayers()
          .stream()
          .filter(this::canView)
          .forEach(this::showTo);
    }

    public void showTo(@NotNull Player player) {
        if (viewers.contains(player.getUniqueId()) || !canView(player)) return;

        lines.forEach(line -> line.showTo(player));
        viewers.add(player.getUniqueId());
    }

    public void hideTo(@NotNull Player player) {
        if (!viewers.contains(player.getUniqueId())) return;

        lines.forEach(line -> line.hideTo(player));
        viewers.remove(player.getUniqueId());
    }

    public void hideToAll() {
        lines.forEach(line -> forEachViewers(line::hideTo));
        viewers.clear();
    }

    public boolean canAutoUpdate() {
        return autoUpdate && System.currentTimeMillis() >= updateInterval;
    }

    public void refreshAutoUpdateInterval() {
        this.updateInterval = System.currentTimeMillis() + updateInterval;
    }

    public void forEachViewers(@NotNull Consumer<Player> action) {
        for (UUID viewerId : viewers) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    public boolean matchesWithWorld(@NotNull World world) {
        return world.getName().equals(location.getWorld().getName());
    }
}

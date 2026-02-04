package io.github.minehollow.mines.util;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import io.github.minehollow.minecraft.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record MinePreSelection(@NotNull Location pos1, @NotNull Location pos2) {

    public LongSet getAllChunksInSelection() {
        LongSet chunks = new LongOpenHashSet();
        int x1 = pos1.getBlockX() >> 4;
        int z1 = pos1.getBlockZ() >> 4;
        int x2 = pos2.getBlockX() >> 4;
        int z2 = pos2.getBlockZ() >> 4;

        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                long chunkKey = ChunkUtil.pack(x, z);
                chunks.add(chunkKey);
            }
        }
        return chunks;
    }


    public int getMinX() {
        return Math.min(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMinY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }

    public int getMinZ() {
        return Math.min(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public int getMaxX() {
        return Math.max(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMaxY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    public int getMaxZ() {
        return Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }


    public static MinePreSelection getCurrent(@NotNull Player player) {
        final var actor = BukkitAdapter.adapt(player);
        final var session = WorldEdit.getInstance().getSessionManager().get(actor);

        try {
            Region region = session.getSelection(actor.getWorld());
            if (region == null) {
                return null;
            }

            var loc1 = BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint());
            var loc2 = BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint());
            return new MinePreSelection(loc1, loc2);
        } catch (IncompleteRegionException e) {
            return null;
        }
    }
}
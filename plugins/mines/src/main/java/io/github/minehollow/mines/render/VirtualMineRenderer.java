package io.github.minehollow.mines.render;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.mine.VirtualMineBlockResolver;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.UUID;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VirtualMineRenderer {

    @Getter
    private final String worldName;
    private final VirtualMineBlockResolver resolver;

    public VirtualMineRenderer(@NotNull String worldName, @NotNull VirtualMineBlockResolver resolver) {
        this.worldName = worldName;
        this.resolver = resolver;
    }

    public void sendChunkOverlay(@NotNull Player viewer, @NotNull MineInstance instance, int chunkX, int chunkZ) {
        final World world = viewer.getWorld();
        if (!this.isMineWorld(world)) {
            return;
        }

        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null || !area.intersectsChunk(chunkX, chunkZ)) {
            return;
        }

        this.sendChunkSectionBatch(viewer, instance, chunkX, chunkZ, true);
    }

    public void sendNearChunks(@NotNull Player viewer, @NotNull MineInstance instance) {
        final World world = viewer.getWorld();
        if (!this.isMineWorld(world)) {
            return;
        }

        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null) {
            return;
        }

        final int centerChunkX = viewer.getLocation().getBlockX() >> 4;
        final int centerChunkZ = viewer.getLocation().getBlockZ() >> 4;
        final int viewDistance = Math.max(2, Bukkit.getViewDistance());

        for (int chunkX = centerChunkX - viewDistance; chunkX <= centerChunkX + viewDistance; chunkX++) {
            for (int chunkZ = centerChunkZ - viewDistance; chunkZ <= centerChunkZ + viewDistance; chunkZ++) {
                if (!area.intersectsChunk(chunkX, chunkZ) || !world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                this.sendChunkSectionBatch(viewer, instance, world, area, chunkX, chunkZ, true);
            }
        }
    }

    public void sendAllMiningChunks(@NotNull Player viewer, @NotNull MineInstance instance) {
        final World world = viewer.getWorld();
        if (!this.isMineWorld(world)) {
            return;
        }

        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null) {
            return;
        }

        area.forEachChunk((chunkX, chunkZ) -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return;
            }

            this.sendChunkSectionBatch(viewer, instance, world, area, chunkX, chunkZ, true);
        });
    }

    public void sendWorldChunk(@NotNull Player viewer, @NotNull MineInstance instance, int chunkX, int chunkZ) {
        final World world = viewer.getWorld();
        if (!this.isMineWorld(world)) {
            return;
        }

        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null || !area.intersectsChunk(chunkX, chunkZ)) {
            return;
        }

        this.sendChunkSectionBatch(viewer, instance, chunkX, chunkZ, false);
    }

    public void restoreNearChunks(@NotNull Player viewer, @NotNull MineInstance instance) {
        final World world = viewer.getWorld();
        if (!this.isMineWorld(world)) {
            return;
        }

        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null) {
            return;
        }

        final int centerChunkX = viewer.getLocation().getBlockX() >> 4;
        final int centerChunkZ = viewer.getLocation().getBlockZ() >> 4;
        final int viewDistance = Math.max(2, Bukkit.getViewDistance());

        for (int chunkX = centerChunkX - viewDistance; chunkX <= centerChunkX + viewDistance; chunkX++) {
            for (int chunkZ = centerChunkZ - viewDistance; chunkZ <= centerChunkZ + viewDistance; chunkZ++) {
                if (!area.intersectsChunk(chunkX, chunkZ) || !world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                this.sendChunkSectionBatch(viewer, instance, world, area, chunkX, chunkZ, false);
            }
        }
    }

    public void sendBlockToMembers(@NotNull MineInstance instance, int x, int y, int z, @NotNull BlockData data) {
        final int stateId = SpigotConversionUtil.fromBukkitBlockData(data).getGlobalId();
        final PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        final var packet = new WrapperPlayServerBlockChange(new Vector3i(x, y, z), stateId);
        for (UUID viewerId : instance.getMembers()) {
            final Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline() || !this.isMineWorld(viewer.getWorld())) {
                continue;
            }

            pm.sendPacket(viewer, packet);
        }
    }

    public void sendBlockToViewer(@NotNull Player viewer, int x, int y, int z, @NotNull BlockData data) {
        if (!this.isMineWorld(viewer.getWorld())) {
            return;
        }

        if (!viewer.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }

        final int stateId = SpigotConversionUtil.fromBukkitBlockData(data).getGlobalId();
        final var packet = new WrapperPlayServerBlockChange(new Vector3i(x, y, z), stateId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private void sendChunkSectionBatch(@NotNull Player viewer, @NotNull MineInstance instance, int chunkX, int chunkZ, boolean virtual) {
        final World world = viewer.getWorld();
        final SimpleCuboidArea area = instance.getDefinition().getMiningArea();
        if (area == null) {
            return;
        }

        this.sendChunkSectionBatch(viewer, instance, world, area, chunkX, chunkZ, virtual);
    }

    private void sendChunkSectionBatch(@NotNull Player viewer, @NotNull MineInstance instance, @NotNull World world, @NotNull SimpleCuboidArea area, int chunkX, int chunkZ, boolean virtual) {
        final Int2ObjectMap<ObjectList<WrapperPlayServerMultiBlockChange.EncodedBlock>> sectionBlocks = new Int2ObjectOpenHashMap<>();
        area.forEachBlocksInChunk(
            chunkX, chunkZ, (x, y, z) -> {
                final int stateId;
                if (virtual) {
                    stateId = SpigotConversionUtil.fromBukkitBlockData(this.resolver.resolve(instance, x, y, z)).getGlobalId();
                } else {
                    stateId = SpigotConversionUtil.fromBukkitBlockData(world.getBlockAt(x, y, z).getBlockData()).getGlobalId();
                }

                sectionBlocks.computeIfAbsent(y >> 4, $ -> new ObjectArrayList<>())
                    .add(new WrapperPlayServerMultiBlockChange.EncodedBlock(stateId, x & 0xF, y & 0xF, z & 0xF));
            }
        );

        final PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        for (Int2ObjectMap.Entry<ObjectList<WrapperPlayServerMultiBlockChange.EncodedBlock>> entry : sectionBlocks.int2ObjectEntrySet()) {
            final var blocks = entry.getValue();
            final var packet = new WrapperPlayServerMultiBlockChange(
                new Vector3i(chunkX, entry.getIntKey(), chunkZ),
                false,
                blocks.toArray(new WrapperPlayServerMultiBlockChange.EncodedBlock[0])
            );
            pm.sendPacket(viewer, packet);
        }
    }

    public boolean isMineWorld(@Nullable World world) {
        return world != null && world.getName().equalsIgnoreCase(this.worldName);
    }
}

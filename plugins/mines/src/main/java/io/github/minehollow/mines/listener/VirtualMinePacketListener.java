package io.github.minehollow.mines.listener;

import com.destroystokyo.paper.MaterialTags;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.render.VirtualMineRenderer;
import io.github.minehollow.mines.service.VirtualMineService;
import io.github.minehollow.mines.util.CachedBlockData;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class VirtualMinePacketListener extends PacketListenerAbstract {

    private final MinesPlugin plugin;
    private final VirtualMineService mineService;
    private final VirtualMineRenderer renderer;

    public VirtualMinePacketListener(
        @NotNull MinesPlugin plugin,
        @NotNull VirtualMineService mineService,
        @NotNull VirtualMineRenderer renderer
    ) {
        this.plugin = plugin;
        this.mineService = mineService;
        this.renderer = renderer;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) {
            return;
        }

        final Player player = event.getPlayer();

        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance == null || !this.mineService.getRenderer().isMineWorld(player.getWorld())) {
            return;
        }

        final WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        final Vector3i pos = wrapper.getBlockPosition();

        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final var miningArea = instance.getDefinition().getMiningArea();
        if (miningArea == null || !miningArea.contains(x, y, z)) {
            return;
        }

        final DiggingAction action = wrapper.getAction();
        if (action != DiggingAction.START_DIGGING
            && action != DiggingAction.CANCELLED_DIGGING
            && action != DiggingAction.FINISHED_DIGGING) {
            return;
        }

        // Acknowledge dig sequence to keep client prediction in sync when we cancel virtual mining.
        this.acknowledgeDigSequence(player, wrapper);

        // Keep all mining state virtual by preventing vanilla block break processing.
        event.setCancelled(true);

        final MineInstance liveInstance = this.mineService.findByPlayer(player.getUniqueId());
        if (liveInstance == null || !this.mineService.getRenderer().isMineWorld(player.getWorld())) {
            return;
        }

        if (!isPickaxe(player.getInventory().getItemInMainHand())) {
            this.resyncVirtualBlock(liveInstance, player, x, y, z, true);
            return;
        }

        if (action != DiggingAction.START_DIGGING) {
            return;
        }

        if (!this.mineService.handleVirtualBreak(player, player.getWorld().getBlockAt(x, y, z))) {
            this.resyncVirtualBlock(liveInstance, player, x, y, z, false);
        } else {
            this.renderer.sendBlockToMembers(liveInstance, x, y, z, CachedBlockData.get(Material.AIR));
            this.renderer.sendBlockToViewer(player, x, y, z, CachedBlockData.get(Material.AIR));
        }
    }

    private boolean isPickaxe(ItemStack itemStack) {
        return itemStack != null && MaterialTags.PICKAXES.isTagged(itemStack.getType());
    }

    private void resyncVirtualBlock(@NotNull MineInstance instance, @NotNull Player player, int x, int y, int z, boolean forceChunkOverlay) {
        final var virtualBlock = this.mineService.getResolver().resolve(instance, x, y, z);
        this.mineService.getRenderer().sendBlockToMembers(instance, x, y, z, virtualBlock);
        this.mineService.getRenderer().sendBlockToViewer(player, x, y, z, virtualBlock);

        if (!forceChunkOverlay) {
            return;
        }

        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        this.mineService.getRenderer().sendChunkOverlay(player, instance, chunkX, chunkZ);

    }

    private void acknowledgeDigSequence(@NotNull Player player, @NotNull WrapperPlayClientPlayerDigging wrapper) {
        final int sequence = wrapper.getSequence();
        if (sequence < 0) {
            return;
        }

        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }

        craftPlayer.getHandle().connection.ackBlockChangesUpTo(sequence);
    }

    @Override
    @SuppressWarnings("all")
    public void onPacketSend(PacketSendEvent event) {
        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance == null || !this.mineService.getRenderer().isMineWorld(player.getWorld())) {
            return;
        }

        final var miningArea = instance.getDefinition().getMiningArea();
        if (miningArea == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            final WrapperPlayServerChunkData chunkPacket = new WrapperPlayServerChunkData(event);
            final int chunkX = chunkPacket.getColumn().getX();
            final int chunkZ = chunkPacket.getColumn().getZ();
            if (!miningArea.intersectsChunk(chunkX, chunkZ)) {
                return;
            }

            event.getTasksAfterSend().add(() ->
                this.mineService.getRenderer().sendChunkOverlay(player, instance, chunkX, chunkZ)
            );
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.BLOCK_CHANGE) {
            return;
        }

        final WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(event);
        final Vector3i pos = blockChange.getBlockPosition();
        if (!miningArea.contains(pos.getX(), pos.getY(), pos.getZ())) {
            return;
        }

        final var virtualBlock = this.mineService.getResolver().resolve(instance, pos.getX(), pos.getY(), pos.getZ());
        final int blockId = SpigotConversionUtil.fromBukkitBlockData(virtualBlock).getGlobalId();
        blockChange.setBlockID(blockId);
        event.markForReEncode(true);
    }
}

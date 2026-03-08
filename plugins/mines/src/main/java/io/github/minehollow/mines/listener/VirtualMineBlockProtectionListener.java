package io.github.minehollow.mines.listener;

import com.destroystokyo.paper.MaterialTags;
import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.service.VirtualMineService;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class VirtualMineBlockProtectionListener implements Listener {

    private final VirtualMineService mineService;

    public VirtualMineBlockProtectionListener(@NotNull VirtualMineService mineService) {
        this.mineService = mineService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final MineInstance instance = this.resolveInstanceInMiningArea(player, block);
        if (instance == null) {
            return;
        }

        if (this.isPickaxe(player.getInventory().getItemInMainHand())) {
            return;
        }

        event.setCancelled(true);
        this.resyncVirtualBlock(instance, block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final MineInstance instance = this.resolveInstanceInMiningArea(player, block);
        if (instance == null) {
            return;
        }

        // Never allow vanilla break in mining area; virtual break is handled by packet flow.
        event.setDropItems(false);
        event.setExpToDrop(0);
        event.setCancelled(true);
        this.resyncVirtualBlock(instance, block);
    }

    private MineInstance resolveInstanceInMiningArea(@NotNull Player player, @NotNull Block block) {
        if (!this.mineService.getRenderer().isMineWorld(block.getWorld())) {
            return null;
        }

        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance == null) {
            return null;
        }

        final SimpleCuboidArea miningArea = instance.getDefinition().getMiningArea();
        if (miningArea == null) {
            return null;
        }

        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();
        if (!miningArea.contains(x, y, z)) {
            return null;
        }

        return instance;
    }

    private void resyncVirtualBlock(@NotNull MineInstance instance, @NotNull Block block) {
        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();

        final var virtualBlock = this.mineService.getResolver().resolve(instance, x, y, z);
        this.mineService.getRenderer().sendBlockToMembers(instance, x, y, z, virtualBlock);
    }

    private boolean isPickaxe(ItemStack itemStack) {
        return itemStack != null && MaterialTags.PICKAXES.isTagged(itemStack.getType());
    }
}


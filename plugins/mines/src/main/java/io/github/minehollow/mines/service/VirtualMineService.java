package io.github.minehollow.mines.service;

import com.destroystokyo.paper.MaterialTags;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.event.VirtualMineBlockBreakEvent;
import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.instance.MineInstanceManager;
import io.github.minehollow.mines.mine.MineDefinition;
import io.github.minehollow.mines.mine.MineDefinitionRegistry;
import io.github.minehollow.mines.mine.MineSpawnPoint;
import io.github.minehollow.mines.mine.VirtualMineBlockResolver;
import io.github.minehollow.mines.render.VirtualMineRenderer;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.wallet.WalletTransactionContext;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public final class VirtualMineService {

    private final MineDefinitionRegistry definitionRegistry;
    private final MineInstanceManager instanceManager;
    private final VirtualMineRenderer renderer;
    private final VirtualMineBlockResolver resolver = new VirtualMineBlockResolver();

    public VirtualMineService(
        @NotNull MineDefinitionRegistry definitionRegistry,
        @NotNull MineInstanceManager instanceManager,
        @NotNull VirtualMineRenderer renderer
    ) {
        this.definitionRegistry = definitionRegistry;
        this.instanceManager = instanceManager;
        this.renderer = renderer;
    }

    public @Nullable MineInstance createOrJoinOwnedMine(@NotNull Player owner, int mineLevel) {
        MineDefinition definition = this.definitionRegistry.findById(this.definitionRegistry.getDefaultMineId());
        if (definition == null || definition.getMiningArea() == null) {
            definition = this.definitionRegistry.getAll()
                .stream()
                .filter(candidate -> candidate.getMiningArea() != null)
                .findFirst()
                .orElse(null);
        }
        if (definition == null) {
            return null;
        }

        final MineInstance instance = this.instanceManager.createOrGet(owner.getUniqueId(), definition.getId(), definition, mineLevel);
        this.instanceManager.invite(owner.getUniqueId(), owner.getUniqueId());
        this.renderer.sendNearChunks(owner, instance);
        return instance;
    }

    public boolean invite(@NotNull UUID ownerId, @NotNull Player target) {
        final MineInstance ownerInstance = this.instanceManager.findByOwner(ownerId);
        if (ownerInstance == null) {
            return false;
        }

        final MineInstance previous = this.instanceManager.findByMember(target.getUniqueId());
        if (previous != null && !previous.getOwnerId().equals(ownerId)) {
            this.renderer.restoreNearChunks(target, previous);
        }

        final boolean invited = this.instanceManager.invite(ownerId, target.getUniqueId());
        this.renderer.sendNearChunks(target, ownerInstance);
        return invited;
    }

    public boolean leave(@NotNull Player player) {
        final MineInstance instance = this.instanceManager.findByMember(player.getUniqueId());
        final boolean left = this.instanceManager.leave(player.getUniqueId());
        if (!left || instance == null) {
            return false;
        }

        this.renderer.restoreNearChunks(player, instance);
        return true;
    }

    public boolean reset(@NotNull UUID ownerId) {
        final MineInstance instance = this.instanceManager.findByOwner(ownerId);
        if (instance == null) {
            return false;
        }

        final SimpleCuboidArea miningArea = instance.getDefinition().getMiningArea();
        if (miningArea == null) {
            return false;
        }

        final World world = Bukkit.getWorld(this.renderer.getWorldName());

        // Move members out of the mining pit before regenerating virtual blocks.
        for (UUID memberId : instance.getMembers()) {
            final Player player = Bukkit.getPlayer(memberId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (world == null || player.getWorld() != world) {
                continue;
            }

            final int x = player.getLocation().getBlockX();
            final int y = player.getLocation().getBlockY();
            final int z = player.getLocation().getBlockZ();
            if (!miningArea.contains(x, y, z)) {
                continue;
            }

            player.teleport(this.resolveMineSpawn(instance));
            player.sendMessage(MinesPlugin.messages().mm("mine.reset_teleport"));
        }

        instance.reset();

        for (UUID memberId : instance.getMembers()) {
            final Player player = Bukkit.getPlayer(memberId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            this.renderer.sendAllMiningChunks(player, instance);
        }

        return true;
    }

    private @NotNull Location resolveMineSpawn(@NotNull MineInstance instance) {
        return this.resolveMineSpawn(instance.getDefinition());
    }

    public @NotNull Location resolveMineSpawn(@NotNull MineDefinition definition) {
        final World world = Bukkit.getWorld(this.renderer.getWorldName());
        if (world == null) {
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }

        final MineSpawnPoint spawnPoint = definition.getSpawnPoint();
        if (spawnPoint != null) {
            return spawnPoint.toLocation(world);
        }

        final SimpleCuboidArea globalArea = definition.getGlobalArea();
        return new Location(
            world,
            (globalArea.getMinX() + globalArea.getMaxX()) / 2.0D + 0.5D,
            globalArea.getMinY() + 1.0D,
            (globalArea.getMinZ() + globalArea.getMaxZ()) / 2.0D + 0.5D
        );
    }

    public boolean teleportToDefinition(@NotNull Player player, @NotNull MineDefinition definition) {
        if (definition.getMiningArea() == null) {
            return false;
        }

        final MineInstance previousInstance = this.findByPlayer(player.getUniqueId());
        final MineInstance targetInstance = this.instanceManager.createOrGet(
            player.getUniqueId(),
            definition.getId(),
            definition,
            1
        );
        this.instanceManager.invite(player.getUniqueId(), player.getUniqueId());

        if (!player.teleport(this.resolveMineSpawn(definition))) {
            return false;
        }

        if (previousInstance != null && !previousInstance.getOwnerId().equals(targetInstance.getOwnerId())) {
            this.renderer.restoreNearChunks(player, previousInstance);
        }

        // Send virtual blocks immediately after teleport so player does not need to move first.
        Tasks.runAsync(() -> {
            if (!player.isOnline()) {
                return;
            }

            final MineInstance liveInstance = this.findByPlayer(player.getUniqueId());
            if (liveInstance == null) {
                return;
            }

            this.renderer.sendNearChunks(player, liveInstance);
            this.renderer.sendChunkOverlay(
                player,
                liveInstance,
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4
            );
        });

        return true;
    }

    public @Nullable MineInstance findByPlayer(@NotNull UUID playerId) {
        return this.instanceManager.findByMember(playerId);
    }

    public @Nullable MineInstance autoEnterByLocation(@NotNull Player player, @NotNull Location location) {
        final MineInstance current = this.findByPlayer(player.getUniqueId());
        if (current != null) {
            return current;
        }

        if (!this.renderer.isMineWorld(location.getWorld())) {
            return null;
        }

        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        for (MineDefinition definition : this.definitionRegistry.getAll()) {
            if (definition.getMiningArea() == null) {
                continue;
            }

            if (!definition.getGlobalArea().contains(x, y, z)) {
                continue;
            }

            return this.createOrJoinOwnedMine(player, 1);
        }

        return null;
    }

    public boolean handleVirtualBreak(@NotNull Player player, @NotNull Block block) {
        if (!this.isPickaxe(player.getInventory().getItemInMainHand())) {
            return false;
        }

        final MineInstance instance = this.findByPlayer(player.getUniqueId());
        if (instance == null) {
            return false;
        }

        if (!this.renderer.isMineWorld(block.getWorld())) {
            return false;
        }

        final SimpleCuboidArea miningArea = instance.getDefinition().getMiningArea();
        if (miningArea == null) {
            return false;
        }

        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();
        if (!miningArea.contains(x, y, z)) {
            return false;
        }

        if (instance.isMined(x, y, z)) {
            return false;
        }

        final BlockData brokenBlock = this.resolver.resolve(instance, x, y, z);
        instance.markMined(x, y, z);

        final VirtualMineBlockBreakEvent event = new VirtualMineBlockBreakEvent(player, instance, x, y, z, brokenBlock);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        this.applyConfiguredCurrencyRewards(player, instance);
        return true;
    }

    private void applyConfiguredCurrencyRewards(@NotNull Player player, @NotNull MineInstance instance) {
        final var gainValues = instance.getDefinition().getCurrencyGainValues();
        if (gainValues == null || gainValues.isEmpty()) {
            return;
        }

    }

    private boolean isPickaxe(@Nullable ItemStack itemStack) {
        return itemStack != null && MaterialTags.PICKAXES.isTagged(itemStack.getType());
    }
}

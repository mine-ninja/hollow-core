/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.common.base.Preconditions;
import gg.nerdzone.prison.mining.api.context.MineAttributeContainer;
import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.api.context.MineBlockBreakSource;
import gg.nerdzone.prison.mining.api.context.state.MineContextState;
import gg.nerdzone.prison.mining.api.events.block.MineBlockBreakEvent;
import gg.nerdzone.prison.mining.api.events.block.MinePostBlockBreakEvent;
import gg.nerdzone.prison.mining.context.model.block.PaletteBlockArray;
import gg.nerdzone.prison.mining.enums.MineBreakReason;
import gg.nerdzone.prison.mining.impl.MinePacketServiceImpl;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.block.MinePaletteBlock;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.packet.util.MineBlockUtil;
import gg.nerdzone.prison.mining.services.MiningPacketService;
import gg.nerdzone.prison.model.PrisonUserProfile;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;
import me.lucko.helper.Events;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Default implementation of {@link MineBlockBreakContext}.
 */
@ToString
public class MineBlockBreakContextImpl implements MineBlockBreakContext {
    private static final int AIR = 0;

    private final MineBlockPosition sourcePosition;

    private final Material sourceMaterial;

    private final MineBlockBreakSource<?> source;

    private final Mine mine;

    private final MiningUser user;

    // Optimized block storage O(1) based using dedicated array class
    private final PaletteBlockArray blocksArray;

    private final Set<MineBlockBreakSource<?>> subSources = ConcurrentHashMap.newKeySet();

    private final MineAttributeContainer attributeContainer = MineAttributeContainerMap.create();

    private final AtomicInteger blocksCount = new AtomicInteger(0);
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private final AtomicBoolean autoSendOnFlush = new AtomicBoolean(true);
    private final AtomicBoolean experienceEnabled = new AtomicBoolean(false);

    private final AtomicInteger arrayIndex = new AtomicInteger(0); // Used for palette blocks array size

    private final int playerLevel;

    /**
     * Contextual state shared across enchantment and block handlers. This forms a circular reference with {@code MineContextStateImpl}, which holds a
     * back-reference to this. Safe because neither constructor uses the other before initialization completes.
     */
    private final AtomicReference<MineContextStateImpl> state;

    public static @NotNull MineBlockBreakContextImpl create(@NonNull MineBlockBreakEvent event) {
        return create(event.getPosition(), event.getPreviousMaterial(), event.getMine(), event.getMiningUser(), event.getPlayerLevel());
    }

    public static @NotNull MineBlockBreakContextImpl create(
        MineBlockPosition sourcePosition,
        Material sourceMaterial,
        Mine mine,
        MiningUser user,
        int playerLevel
    ) {
        return create(sourcePosition, sourceMaterial, mine, user, playerLevel, 1);
    }

    public static @NotNull MineBlockBreakContextImpl create(
        MineBlockPosition sourcePosition,
        Material sourceMaterial,
        Mine mine,
        MiningUser user,
        int playerLevel,
        int initialBlockCapacity
    ) {
        Objects.requireNonNull(sourcePosition, "sourcePosition");
        Objects.requireNonNull(sourceMaterial, "sourceMaterial");
        Objects.requireNonNull(mine, "mine");
        return new MineBlockBreakContextImpl(sourcePosition, sourceMaterial, mine, user, playerLevel, initialBlockCapacity);
    }

    private MineBlockBreakContextImpl(
        MineBlockPosition sourcePosition,
        Material sourceMaterial,
        Mine mine,
        MiningUser user,
        int playerLevel,
        int initialBlockCapacity
    ) {
        this.sourcePosition = sourcePosition;
        this.mine = mine;
        this.playerLevel = playerLevel;
        this.user = user;
        this.sourceMaterial = sourceMaterial;
        this.source = user == null
                      ? new MineBlockBreakSourceImpl(sourcePosition)
                      : new MinePlayerBreakSourceImpl(mine, user);

        this.state = new AtomicReference<>(new MineContextStateImpl(this));

        this.blocksArray = new PaletteBlockArray(initialBlockCapacity, this.getMineArea().getSize() + 1);

        this.addAirBlock(sourcePosition); // Set block source in mine area

    }

    @Override
    public @NotNull MineBlockPosition getBlockPosition() {
        return this.sourcePosition;
    }

    @Override
    public @NotNull Material getBlockMaterial() {
        return this.sourceMaterial;
    }

    @Override
    public @NotNull MineBlockBreakSource<?> getSource() {
        return this.source;
    }

    @Override
    public @NotNull Mine getMine() {
        return this.mine;
    }

    @Override
    public @NotNull MineArea getMineArea() {
        return this.mine.getArea();
    }

    @Override
    public @NotNull PrisonUserProfile getUserProfile() {
        return Objects.requireNonNull(
            this.getMiningUser().getProfile(),
            "Username is not associated with a valid profile. (%s)".formatted(this.getMiningUser().getName())
        );
    }

    @Override
    public int getPlayerLevel() {
        return this.playerLevel;
    }

    public @NotNull MiningUser getMiningUser() {
        return this.user;
    }

    @Override
    public @Unmodifiable MinePaletteBlock[] getPaletteBlocks() {
        return this.blocksArray.toArray();
    }

    @Override
    public @NotNull MineBlockBreakContext setAutoSendOnFlush(boolean enabled) {
        this.autoSendOnFlush.set(enabled);
        return this;
    }

    @Override
    public boolean isAutoSendOnFlush() {
        return this.autoSendOnFlush.get();
    }

    @Override
    public boolean isExperienceEnabled() {
        return this.experienceEnabled.get();
    }

    @Override
    public @NotNull MineBlockBreakContext setExperienceEnabled(boolean enabled) {
        this.experienceEnabled.set(enabled);
        return this;
    }

    @Override
    public void sendBlocks(boolean force, boolean clear, boolean resetBlockCount) {
        Preconditions.checkState(
            !(this.autoSendOnFlush.get() && !force),
            "Cannot send blocks when auto send is enabled. Consider using setAutoSendOnFlush() to disable it."
        );

        Preconditions.checkState(
            !(this.flushed.get() && !force),
            "This context has already been flushed. Cannot send blocks."
        );

        if (this.blocksArray.isEmpty()) {
            return;
        }

        final Mine mine = this.getMine();
        final Player player = this.getPlayer();

        // Silent to prevent circular flow
        MineBlockUtil.handleBlockBreak(mine, this.user, player, true, this.getPaletteBlocks());

        if (!clear) {
            return;
        }

        this.blocksArray.clear();

        if (!resetBlockCount) {
            return;
        }

        this.dispatchExperienceBlocks();
        this.blocksCount.set(0);
    }

    @Override
    public void sendPacket(@NotNull PacketWrapper<?> packetWrapper, boolean forceSource) {
        final MiningPacketService packetService = MinePacketServiceImpl.get();
        packetService.simulatePacket(this.getPlayer(), packetWrapper, forceSource);
    }

    @Override
    public boolean addBlock(@NonNull MinePaletteBlock block, @NonNull MineBlockBreakContext.BlockInsertRule rule) {
        if (!this.mine.isMiningArea(block.x(), block.y(), block.z())) {
            return false;
        }

        if (!this.blocksArray.addBlock(block, rule)) {
            return false; // No change in block
        }

        this.blocksCount.incrementAndGet();
        return true;
    }

    @Override
    public boolean addAirBlock(@NonNull MineBlockPosition position, @NonNull BlockInsertRule rule) {
        if (!this.mine.isMiningArea(position.x(), position.y(), position.z())) {
            return false;
        }

        return this.addBlock(new MinePaletteBlock(position, AIR), rule);
    }

    @Override
    public void removeBlock(@NotNull MineBlockPosition position) {
        this.blocksArray.removeBlock(position.x(), position.y(), position.z());
    }

    @Override
    public int getBlocksCount() {
        return this.blocksCount.get();
    }

    @Override
    public void transformBlocksCount(@NotNull UnaryOperator<Integer> transformer) {
        this.blocksCount.set(transformer.apply(this.blocksCount.get()));
    }

    @Override
    public @NotNull MineAttributeContainer getAttributeContainer() {
        return this.attributeContainer;
    }

    @Override
    public @NotNull MineContextState getState() {
        return this.state.get();
    }

    @Override
    public @NotNull MineBlockBreakContext withSource(@NotNull MineBlockBreakSource<?> source) {
        this.subSources.add(source);
        return new SubContext(source, this);
    }

    @Override
    public @NotNull MineBlockBreakContext withSourcePosition(@NotNull MineBlockPosition position) {
        return new SubContext(this.source, this) {
            @Override
            public @NotNull MineBlockPosition getBlockPosition() {
                return position;
            }
        };
    }

    @Override
    public void flush() {
        if (!this.flushed.compareAndSet(false, true)) {
            return;
        }

        // System.out.println("Memory stats: " + this.blocksArray.getMemoryStats());

        this.dispatchExperienceBlocks();

        this.attributeContainer.clear();
        this.blocksArray.flush();
        this.state.set(null);
        this.blocksCount.set(0);
        this.subSources.clear();
    }

    public @NotNull MineBlockBreakEvent callBreakEvent() {
        return Events.callAndReturn(new MineBlockBreakEvent(this));
    }

    public @NotNull MinePostBlockBreakEvent callPostBreakEvent() {
        final MineBreakReason reason = (this.getPlayer() != null) ? MineBreakReason.PLAYER : MineBreakReason.UNKNOWN;

        return Events.callAndReturn(new MinePostBlockBreakEvent(
            this.getPlayer(), this.getMine(), reason,
            this.getPaletteBlocks()
        ));
    }

    @Internal
    private void dispatchExperienceBlocks() {
        final int blockCount = this.getBlocksCount();
        if (this.isExperienceEnabled() && blockCount > 0) {
            this.getUserProfile().addExperience(blockCount);
            this.callPostBreakEvent();
        }
    }

    @RequiredArgsConstructor
    private static class SubContext implements MineBlockBreakContext {

        private final MineBlockBreakSource<?> source;

        @Delegate(types = MineBlockBreakContext.class)
        private final MineBlockBreakContext root;

        @Override
        public @NotNull MineBlockBreakSource<?> getSource() {
            return this.source;
        }
    }
}

/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model.block;

import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.block.MinePaletteBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe, memory-optimized storage for {@code MinePaletteBlock} arrays. Automatically grows and shrinks based on usage patterns to minimize memory
 * footprint. This class provides O(1) lookup for block positions using a HashMap, while maintaining an array for efficient iteration and memory usage.
 * <p>
 * When removing blocks, it's not O(1) because it needs to search through the array, but this is acceptable for most use cases.
 */
public final class PaletteBlockArray {

    private static final float GROWTH_FACTOR = 1.5f;
    private static final int SHRINK_THRESHOLD_RATIO = 4; // Shrink when usage < capacity/4

    // O(1) lookup for block positions
    private final Map<Long, Integer> positionIndexMap = new HashMap<>();

    private final Object lock = new Object();

    private final int initialCapacity;
    private final int maxCapacity;

    private MinePaletteBlock[] array;
    private int size = 0;

    /**
     * Creates a new MinePaletteBlockArray with specified maximum capacity.
     *
     * @param initialCapacity Initial capacity of the array (will grow if needed)
     * @param maxCapacity     Max number of blocks this array can hold
     */
    public PaletteBlockArray(int initialCapacity, int maxCapacity) {
        this.initialCapacity = Math.max(initialCapacity, 2); // Ensure initial capacity is at least 2
        this.maxCapacity = maxCapacity;
        this.array = new MinePaletteBlock[initialCapacity];
    }

    /**
     * Adds a block to the array. If a block already exists at the same position, it will be replaced if the rule allows it.
     *
     * @param block The block to add
     * @param rule  Rule to determine if block should be inserted/replaced
     * @return true if the block was added/replaced, false otherwise
     */
    public boolean addBlock(@NotNull MinePaletteBlock block, @NotNull MineBlockBreakContext.BlockInsertRule rule) {
        synchronized (this.lock) {
            // Find existing block at the same position
            final int existingIndex = this.findBlockIndex(block);

            if (existingIndex != -1) {
                final MinePaletteBlock existing = this.array[existingIndex];
                if (!rule.shouldInsert(existing.id(), block.id())) {
                    return false;
                }

                this.array[existingIndex] = block;
                return true;
            }

            if (this.size >= this.maxCapacity) {
                return false;
            }

            if (this.size >= this.array.length) {
                this.growArray();
            }

            // Cache the position index for fast lookup
            this.positionIndexMap.put(block.asLong(), this.size);
            this.array[this.size] = block;
            this.size++;

            return true;
        }
    }

    /**
     * Removes a block at the specified position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if a block was removed, false if no block found
     */
    public boolean removeBlock(int x, int y, int z) {
        synchronized (this.lock) {
            for (int i = 0; i < this.size; i++) {
                final MinePaletteBlock block = this.array[i];
                if (block != null && block.x() == x && block.y() == y && block.z() == z) {
                    // Shift remaining elements left
                    System.arraycopy(this.array, i + 1, this.array, i, this.size - i - 1);
                    this.array[--this.size] = null; // Clear last element and decrement size

                    // Consider shrinking if array is significantly underutilized
                    this.considerShrinking();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Finds a block at the specified position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The block at the position, or null if not found
     */
    @Nullable
    public MinePaletteBlock getBlock(int x, int y, int z) {
        synchronized (this.lock) {
            final long positionKey = MineBlockPosition.asLong(x, y, z);
            final Integer cachedIndex = this.positionIndexMap.get(positionKey);
            if (cachedIndex != null) {
                return this.array[cachedIndex];
            }
        }

        return null;
    }

    /**
     * Returns a snapshot of all blocks in the array. The returned array is a copy and modifications won't affect the original.
     *
     * @return Array of all blocks (exact size, no null elements)
     */
    @NotNull
    public MinePaletteBlock[] toArray() {
        synchronized (this.lock) {
            if (this.size == 0) {
                return new MinePaletteBlock[0];
            }

            final MinePaletteBlock[] snapshot = new MinePaletteBlock[this.size];
            System.arraycopy(this.array, 0, snapshot, 0, this.size);
            return snapshot;
        }
    }

    /**
     * Returns the current number of blocks in the array.
     *
     * @return Number of blocks
     */
    public int size() {
        synchronized (this.lock) {
            return this.size;
        }
    }

    /**
     * Returns true if the array contains no blocks.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        synchronized (this.lock) {
            return this.size == 0;
        }
    }

    /**
     * Applies the given function to each block in the array.
     *
     * @param consumer Function to apply to each block
     */
    public void forEach(@NotNull Consumer<MinePaletteBlock> consumer) {
        synchronized (this.lock) {
            for (int i = 0; i < this.size; i++) {
                if (this.array[i] != null) {
                    consumer.accept(this.array[i]);
                }
            }
        }
    }

    /**
     * Returns memory usage statistics for debugging/monitoring.
     *
     * @return Memory usage info
     */
    @NotNull
    public MemoryStats getMemoryStats() {
        synchronized (this.lock) {
            final int usedSlots = this.size;
            final int totalSlots = this.array.length;
            final int wastedSlots = totalSlots - usedSlots;
            final double utilizationRatio = totalSlots > 0 ? (double) usedSlots / totalSlots : 0.0;

            return new MemoryStats(usedSlots, totalSlots, wastedSlots, utilizationRatio);
        }
    }

    public void flush() {
        this.array = null;
    }

    public void clear() {
        synchronized (this.lock) {
            this.array = new MinePaletteBlock[this.initialCapacity];
            this.size = 0;
            this.positionIndexMap.clear();
        }
    }

    @Internal
    private void growArray() {
        int newCapacity = Math.max(this.initialCapacity, (int) (this.array.length * GROWTH_FACTOR));

        // Don't exceed max capacity
        if (newCapacity > this.maxCapacity) {
            newCapacity = this.maxCapacity;
        }

        // Don't grow if we're already at max
        if (newCapacity <= this.array.length) {
            return;
        }

        final MinePaletteBlock[] newArray = new MinePaletteBlock[newCapacity];
        System.arraycopy(this.array, 0, newArray, 0, this.size);
        this.array = newArray;
    }

    @Internal
    private void considerShrinking() {
        if (this.array.length <= this.initialCapacity) {
            return; // Don't shrink below initial capacity
        }

        if (this.size * SHRINK_THRESHOLD_RATIO < this.array.length) {
            final int newCapacity = Math.max(this.initialCapacity, this.size * 2);
            final MinePaletteBlock[] newArray = new MinePaletteBlock[newCapacity];
            System.arraycopy(this.array, 0, newArray, 0, this.size);
            this.array = newArray;
        }
    }

    private int findBlockIndex(@NotNull MinePaletteBlock target) {
        final Long positionKey = target.asLong();
        final Integer cachedIndex = this.positionIndexMap.get(positionKey);
        if (cachedIndex != null) {
            return cachedIndex;
        }

        return -1;
    }

    /**
     * Memory usage statistics for the array.
     */
    @Getter
    public static final class MemoryStats {
        private final int usedSlots;
        private final int totalSlots;
        private final int wastedSlots;
        private final double utilizationRatio;

        private MemoryStats(int usedSlots, int totalSlots, int wastedSlots, double utilizationRatio) {
            this.usedSlots = usedSlots;
            this.totalSlots = totalSlots;
            this.wastedSlots = wastedSlots;
            this.utilizationRatio = utilizationRatio;
        }

        @Override
        public String toString() {
            return String.format(
                "MemoryStats{used=%d, total=%d, wasted=%d, utilization=%.2f%%}",
                this.usedSlots, this.totalSlots, this.wastedSlots, this.utilizationRatio * 100
            );
        }
    }
}
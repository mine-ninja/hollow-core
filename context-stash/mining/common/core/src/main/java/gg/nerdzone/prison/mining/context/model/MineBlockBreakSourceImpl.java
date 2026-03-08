/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model;

import gg.nerdzone.prison.mining.api.context.MineBlockBreakSource;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link MineBlockBreakSource} for a specific block position in a mine.
 */
@RequiredArgsConstructor
public class MineBlockBreakSourceImpl implements MineBlockBreakSource<MineBlockPosition> {

    private final MineBlockPosition position;

    @Override
    public @NotNull MineBlockPosition getSource() {
        return this.position;
    }
}

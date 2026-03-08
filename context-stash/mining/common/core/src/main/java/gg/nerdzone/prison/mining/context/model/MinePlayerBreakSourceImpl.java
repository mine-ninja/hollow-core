/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model;

import gg.nerdzone.prison.mining.api.context.MineBlockBreakSource.MineBlockPlayerBreakSource;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link MineBlockPlayerBreakSource MinePlayerSource} for a {@link MiningUser} (Player).
 */
@RequiredArgsConstructor
public class MinePlayerBreakSourceImpl implements MineBlockPlayerBreakSource {

    private final MiningUser user;
    private final Mine mine;
    private final boolean isMineOwner;

    protected MinePlayerBreakSourceImpl(Mine mine, MiningUser user) {
        this.user = user;
        this.mine = mine;
        this.isMineOwner = mine.getOwnerId().equals(user.getName());
    }

    @Override
    public Player getSource() {
        return Bukkit.getPlayer(this.user.getName());
    }

    @Override
    public @NotNull Mine getMine() {
        return this.mine;
    }

    @Override
    public @NotNull MiningUser getUser() {
        return this.user;
    }

    @Override
    public boolean isMineOwner() {
        return this.isMineOwner;
    }
}

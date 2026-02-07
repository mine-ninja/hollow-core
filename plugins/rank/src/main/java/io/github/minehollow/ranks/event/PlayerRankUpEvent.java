package io.github.minehollow.ranks.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@Setter
public class PlayerRankUpEvent  extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final UUID playerId;
    private final int oldRank;
    private int newRank;

    public PlayerRankUpEvent(@NotNull UUID playerId, int oldRank, int newRank) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}

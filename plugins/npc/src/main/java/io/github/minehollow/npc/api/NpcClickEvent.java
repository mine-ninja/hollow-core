package io.github.minehollow.npc.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player clicks on an NPC.
 */
public class NpcClickEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Npc npc;
    private final Player player;
    private final NpcClickType clickType;

    public NpcClickEvent(@NotNull Npc npc, @NotNull Player player, @NotNull NpcClickType clickType) {
        super(true); // async
        this.npc = npc;
        this.player = player;
        this.clickType = clickType;
    }

    public @NotNull Npc getNpc() {
        return npc;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull NpcClickType getClickType() {
        return clickType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}


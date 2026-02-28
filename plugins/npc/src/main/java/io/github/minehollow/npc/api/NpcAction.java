package io.github.minehollow.npc.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface NpcAction {
    void execute(@NotNull Player player, @NotNull Npc npc);
    String getType();
}

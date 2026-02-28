package io.github.minehollow.npc.api.actions;

import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Broadcasts a MiniMessage-formatted message to all online players.
 * Supports {@code %player%} placeholder.
 */
public class BroadcastAction implements NpcAction {

    private final String message;

    public BroadcastAction(@NotNull String message) {
        this.message = message;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull Npc npc) {
        String resolved = message.replace("%player%", player.getName());
        Bukkit.broadcast(StringUtils.formatString(resolved));
    }

    @Override
    public @NotNull String getType() {
        return "BROADCAST";
    }

    public @NotNull String getMessage() {
        return message;
    }
}


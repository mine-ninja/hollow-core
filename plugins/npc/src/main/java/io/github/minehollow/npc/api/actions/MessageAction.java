package io.github.minehollow.npc.api.actions;

import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sends a MiniMessage-formatted message to the clicking player.
 * Supports {@code %player%} placeholder.
 */
public class MessageAction implements NpcAction {

    private final String message;

    public MessageAction(@NotNull String message) {
        this.message = message;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull Npc npc) {
        String resolved = message.replace("%player%", player.getName());
        player.sendMessage(StringUtils.formatString(resolved));
    }

    @Override
    public @NotNull String getType() {
        return "MESSAGE";
    }

    public @NotNull String getMessage() {
        return message;
    }
}


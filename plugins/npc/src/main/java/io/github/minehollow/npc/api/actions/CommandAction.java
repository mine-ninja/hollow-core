package io.github.minehollow.npc.api.actions;

import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Executes a command when clicked. Supports {@code %player%} placeholder.
 */
public class CommandAction implements NpcAction {

    public enum Executor {
        CONSOLE,
        PLAYER
    }

    private final Executor executor;
    private final String command;

    public CommandAction(@NotNull Executor executor, @NotNull String command) {
        this.executor = executor;
        this.command = command;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull Npc npc) {
        Tasks.runSync(() -> {
            String resolved = command.replace("%player%", player.getName());
            if (executor == Executor.CONSOLE) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            } else {
                player.performCommand(resolved);
            }
        });
    }

    @Override
    public @NotNull String getType() {
        return "COMMAND";
    }

    public @NotNull Executor getExecutor() {
        return executor;
    }

    public @NotNull String getCommand() {
        return command;
    }
}


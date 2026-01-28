package io.github.minehollow.kits.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KitCommand extends SimpleCommand {
    public KitCommand() {
        super("kit");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        // TODO: Implement GUI menu
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return super.performTabComplete(ctx);
    }

}

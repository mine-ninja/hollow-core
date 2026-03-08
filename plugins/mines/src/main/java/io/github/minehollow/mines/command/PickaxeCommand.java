package io.github.minehollow.mines.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

public class PickaxeCommand extends SimpleCommand {

    public PickaxeCommand() {
        super("picareta");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {

    }
}

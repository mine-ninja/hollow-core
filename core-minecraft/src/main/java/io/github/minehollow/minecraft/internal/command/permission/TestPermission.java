package io.github.minehollow.minecraft.internal.command.permission;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;

import org.jetbrains.annotations.NotNull;

public class TestPermission extends SimpleCommand {
    public TestPermission() {
        super("testperm", "hollow.master");
    }
    
    // /testperm <player> <perm>
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var playerToTest = ctx.getLocalPlayerOrThrow(0, "§cEspecifique um jogador para testar.");
        final var permissionToTest = ctx.getRawArgOrThrow(1, "§cEspecifique uma permissão para testar.");
        
        if (playerToTest.hasPermission(permissionToTest)) {
            ctx.getSender().sendMessage("§aO jogador " + playerToTest.getName() + " tem a permissão: " + permissionToTest);
        } else {
            ctx.getSender().sendMessage("§cO jogador " + playerToTest.getName() + " não tem a permissão: " + permissionToTest);
        }
    }
}

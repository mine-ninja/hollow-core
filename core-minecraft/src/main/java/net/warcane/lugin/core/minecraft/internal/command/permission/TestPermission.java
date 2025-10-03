package net.warcane.lugin.core.minecraft.internal.command.permission;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;

import org.jetbrains.annotations.NotNull;

public class TestPermission extends SimpleCommand {
    public TestPermission() {
        super("testperm", "lugin.master");
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

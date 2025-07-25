package net.warcane.lugin.core.minecraft.command.subcommand;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SimpleSubCommand {

    protected final String subCommandName;

    protected List<String> aliases = List.of();

    protected String permission;
    protected String noPermissionMessage;

    protected boolean playersOnly;
    protected String playersOnlyMessage;


    public SimpleSubCommand(String subCommandName) {
        this.subCommandName = subCommandName;
        this.permission = null;
        this.noPermissionMessage = "§cVocê não tem permissão para executar este comando.";
        this.playersOnly = false;
        this.playersOnlyMessage = "§cEste comando só pode ser executado por jogadores.";
    }

    protected abstract void performSubCommand(CommandContext ctx) throws CommandFailedException;

    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        return List.of();
    }

    public void handleSubCommand(CommandContext ctx) throws CommandFailedException {
        if (permission != null && !ctx.getSender().hasPermission(permission)) {
            ctx.sendMessage(noPermissionMessage);
            return;
        }

        if (playersOnly && ctx.getSenderAsPlayer() == null) {
            ctx.sendMessage(playersOnlyMessage);
            return;
        }

        performSubCommand(ctx);
    }

    public boolean matchesWith(String name) {
        return subCommandName.equalsIgnoreCase(name)
               || aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(name));
    }

    @NotNull
    public String getSubCommandName() {
        return subCommandName;
    }
}

package net.warcane.lugin.core.minecraft.command;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class SimpleCommand extends Command {

    protected String requiredPermission = null;
    protected String noPermissionMessage;


    public SimpleCommand(String name) {
        super(name);
        this.noPermissionMessage = "§cVocê não tem permissão para executar este comando.";
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (requiredPermission != null && !commandSender.hasPermission(requiredPermission)) {
            commandSender.sendMessage(noPermissionMessage);
            return false;
        }

        try {
            CommandContext ctx = new CommandContext(commandSender, strings);
            performCommand(ctx);
            return true;
        } catch (CommandFailedException e) {
            commandSender.sendMessage("§c" + e.getMessage());
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void performCommand(@NotNull CommandContext ctx) throws CommandFailedException;

    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return Collections.emptyList();
    }
}

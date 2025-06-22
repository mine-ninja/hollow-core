package net.warcane.lugin.core.minecraft.command;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class SimpleCommand extends Command {

    protected String requiredPermission;
    protected String noPermissionMessage;

    protected boolean playersOnly;
    protected String playersOnlyMessage;

    protected List<SimpleSubCommand> subCommands = Collections.emptyList();

    public SimpleCommand(String name) {
        super(name);
        this.requiredPermission = null;
        this.noPermissionMessage = "§cVocê não tem permissão para executar este comando.";
        this.playersOnly = false;
        this.playersOnlyMessage = "§cEste comando só pode ser executado por jogadores.";
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] args) {
        try {
            if (requiredPermission != null && !commandSender.hasPermission(requiredPermission)) {
                commandSender.sendMessage(noPermissionMessage);
                return false;
            }

            if (playersOnly && !(commandSender instanceof org.bukkit.entity.Player)) {
                commandSender.sendMessage(playersOnlyMessage);
                return false;
            }

            if (args.length == 0) {
                this.performCommand(new CommandContext(commandSender, args));
                return true;
            }

            if (subCommands.isEmpty()) {
                try {
                    this.performCommand(new CommandContext(commandSender, args));
                } catch (CommandFailedException e) {
                    commandSender.sendMessage("§cErro ao executar o comando: " + e.getMessage());
                } catch (Exception e) {
                    commandSender.sendMessage("§cOcorreu um erro inesperado ao executar o comando.");
                    e.printStackTrace();
                }
                return true;
            }

            var argumentName = args[0].toLowerCase();
            SimpleSubCommand subCommand = this.getSubCommand(argumentName);
            if (subCommand == null) {
                commandSender.sendMessage("§cComando desconhecido: " + argumentName);
                return false;
            }

            var newArguments = Arrays.copyOfRange(args, 1, args.length);
            subCommand.handleSubCommand(new CommandContext(commandSender, newArguments));
            return true;
        } catch (CommandFailedException e) {
            commandSender.sendMessage(e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            commandSender.sendMessage("§cOcorreu um erro interno ao executar este comando.");
            return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        List<String> list = subCommands.stream()
          .map(SimpleSubCommand::getSubCommandName)
          .toList();

        if (args.length == 0) {
            return list;
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], list, new ArrayList<>());
        }

        return performTabComplete(new CommandContext(sender, args));
    }

    public abstract void performCommand(@NotNull CommandContext ctx) throws CommandFailedException;

    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return Collections.emptyList();
    }


    private SimpleSubCommand getSubCommand(String name) {
        if (subCommands.isEmpty()) return null;

        return subCommands.stream()
          .filter(subCommand -> subCommand.matchesWith(name))
          .findFirst()
          .orElse(null);
    }
}

package io.github.minehollow.minecraft.command;

import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class SimpleCommand extends Command {
    public static final List<String> NONE_ARGS = Collections.emptyList();
    
    /**
     * @deprecated Use {@link #setRequiredPermission(String)} instead.
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    protected String requiredPermission = null;
    protected String noPermissionMessage = "§cVocê não tem permissão para executar este comando.";
    
    protected boolean playersOnly = false;
    protected String playersOnlyMessage = "§cEste comando só pode ser executado por jogadores.";

    protected List<SimpleSubCommand> subCommands = Collections.emptyList();

    public SimpleCommand(String name) {
        super(name);
    }
    
    public SimpleCommand(@NotNull String name, String requiredPermission) {
        super(name);
        this.setRequiredPermission(requiredPermission);
    }
    
    protected void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
        super.setPermission(requiredPermission);
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
                }
                catch (CommandFailedException e) {
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
        if (subCommands.isEmpty()) {
            return performTabComplete(new CommandContext(sender, args));
        } else {
            if (args.length == 1) {
                List<String> results = new ArrayList<>();
                for (SimpleSubCommand sc : subCommands) {
                    if (!sc.hasPermission(sender)) continue;
                    for (String name : sc.getAllNames()) {
                        if (StringUtil.startsWithIgnoreCase(name, args[0])) {
                            results.add(name);
                        }
                    }
                }
                return results;
            } else {
                SimpleSubCommand subCommand = getSubCommand(args[0]);
                if (subCommand != null) {
                    var newArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subCommand.performSubCommandTabComplete(new CommandContext(sender, newArgs));
                }
                return Collections.emptyList();
            }
        }
    }

    public abstract void performCommand(@NotNull CommandContext ctx) throws CommandFailedException;

    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return NONE_ARGS;
    }
    
    protected List<String> filterStartingWith(Collection<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) { return List.copyOf(list); }
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (StringUtil.startsWithIgnoreCase(s, prefix)) {
                result.add(s);
            }
        }
        return result;
    }
    
    private SimpleSubCommand getSubCommand(String name) {
        if (subCommands.isEmpty()) return null;
        for (SimpleSubCommand subCommand : subCommands) {
            if (subCommand.matchesWith(name)) {
                return subCommand;
            }
        }
        return null;
    }
}

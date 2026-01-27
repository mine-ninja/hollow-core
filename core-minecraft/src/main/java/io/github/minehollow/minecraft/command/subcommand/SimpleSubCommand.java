package io.github.minehollow.minecraft.command.subcommand;

import com.google.common.collect.ImmutableList;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class SimpleSubCommand {
    protected final String subCommandName;
    protected List<String> aliases = List.of();
    
    protected String permission = null;
    protected String noPermissionMessage = "§cVocê não tem permissão para executar este comando.";
    
    protected boolean playersOnly = false;
    protected String playersOnlyMessage = "§cEste comando só pode ser executado por jogadores.";
    
    private List<String> allNamesCache;
    
    public SimpleSubCommand(String subCommandName) {
        this.subCommandName = subCommandName;
    }
    
    protected abstract void performSubCommand(CommandContext ctx) throws CommandFailedException;
    
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        return SimpleCommand.NONE_ARGS;
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
    
    public boolean hasPermission(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }
    
    public boolean matchesWith(String name) {
        if (subCommandName.equalsIgnoreCase(name)) return true;
        
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean startsWith(String prefix) {
        if (StringUtil.startsWithIgnoreCase(subCommandName, prefix)) {
            return true;
        }
        
        for (String alias : aliases) {
            if (StringUtil.startsWithIgnoreCase(alias, prefix)) {
                return true;
            }
        }
        
        return false;
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
    
    @NotNull
    public String getSubCommandName() {
        return subCommandName;
    }
    
    public List<String> getAllNames() {
        if (allNamesCache == null) {
            allNamesCache = ImmutableList.<String>builder()
                .add(this.subCommandName)
                .addAll(this.aliases)
                .build();
        }
        return allNamesCache;
    }
}

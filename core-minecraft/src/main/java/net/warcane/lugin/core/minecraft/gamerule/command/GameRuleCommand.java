package net.warcane.lugin.core.minecraft.gamerule.command;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.gamerule.CustomGameRule;
import net.warcane.lugin.core.minecraft.gamerule.GameRuleRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to manage custom game rules.
 * Usage: /customgamerule <rule> [value] [world]
 */
public class GameRuleCommand extends SimpleCommand {
    private final BukkitPlatform platform;
    
    public GameRuleCommand(@NotNull BukkitPlatform platform) {
        super("customgamerule");
        this.platform = platform;
        this.setRequiredPermission("lugin.gamerule");
        this.noPermissionMessage = ChatColor.RED + "You don't have permission to use this command.";
        this.setDescription("Manage custom game rules");
        this.setUsage("/customgamerule [rule] [value] [world]");
    }
    
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var args = ctx.getArgs();
        final var sender = ctx.getSender();
        if (args.length == 0) {
            // List all custom game rules
            sender.sendMessage(ChatColor.GOLD + "=== Custom Game Rules ===");
            final var rules = GameRuleRegistry.getAllGameRules();
            if (rules.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No custom game rules registered.");
                return;
            }
            for (CustomGameRule<?> rule : rules) {
                sender.sendMessage(ChatColor.YELLOW + rule.name() + ChatColor.GRAY + " (" + rule.type().getSimpleName() + ")" + ChatColor.WHITE + " - Default: " + ChatColor.GREEN + rule.defaultValue());
                if (!rule.description().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  " + rule.description());
                }
            }
            return;
        }
        
        final String ruleName = args[0];
        final CustomGameRule<?> gameRule = GameRuleRegistry.getGameRule(ruleName);
        
        if (gameRule == null) {
            throw new CommandFailedException(ChatColor.RED + "Unknown custom game rule: " + ruleName + "\n" + ChatColor.GRAY + "Use /customgamerule to see all available rules.");
        }
        
        // Determine target world
        World targetWorld;
        if (args.length >= 3) {
            targetWorld = Bukkit.getWorld(args[2]);
            if (targetWorld == null) {
                throw new CommandFailedException(ChatColor.RED + "Unknown world: " + args[2]);
            }
        } else if (sender instanceof Player player) {
            targetWorld = player.getWorld();
        } else {
            targetWorld = Bukkit.getWorlds().getFirst();
        }
        
        if (args.length == 1) {
            // Get current value
            final Object value = platform.getGameRuleManager().getCustomGameRule(targetWorld, ruleName);
            if (value == null) {
                sender.sendMessage(ChatColor.YELLOW + ruleName + ChatColor.GRAY + " is not set in " + targetWorld.getName() + ". Default: " + ChatColor.GREEN + gameRule.defaultValue());
            } else {
                sender.sendMessage(ChatColor.YELLOW + ruleName + ChatColor.GRAY + " = " + ChatColor.GREEN + value + ChatColor.GRAY + " (in " + targetWorld.getName() + ")");
            }
            return;
        }
        
        // Set value
        final String valueStr = args[1];
        try {
            final Object parsedValue = gameRule.parseValue(valueStr);
            // Use unchecked cast to work around generic wildcard limitation
            @SuppressWarnings("unchecked")
            final Class<Object> type = (Class<Object>) gameRule.type();
            platform.getGameRuleManager().setCustomGameRule(targetWorld, ruleName, type, parsedValue);
            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + ruleName + ChatColor.GREEN + " to " + ChatColor.WHITE + parsedValue + ChatColor.GREEN + " in world " + ChatColor.YELLOW + targetWorld.getName());
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException(ChatColor.RED + "Invalid value: " + e.getMessage() + "\n" + ChatColor.GRAY + "Expected type: " + gameRule.type().getSimpleName());
        }
    }
    
    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();
        
        if (args.length == 1) {
            // Complete game rule names
            return filterStartingWith(GameRuleRegistry.getAllGameRules().stream().map(CustomGameRule::name).collect(Collectors.toList()), args[0]);
        } else if (args.length == 2) {
            // Complete values based on type
            final CustomGameRule<?> rule = GameRuleRegistry.getGameRule(args[0]);
            if (rule != null && rule.type() == Boolean.class) {
                return filterStartingWith(List.of("true", "false"), args[1]);
            }
            return NONE_ARGS;
        } else if (args.length == 3) {
            // Complete world names
            return filterStartingWith(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[2]);
        }
        
        return NONE_ARGS;
    }
}

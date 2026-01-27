package io.github.minehollow.minecraft.gamerule.command;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.gamerule.CustomGameRule;
import io.github.minehollow.minecraft.gamerule.GameRuleRegistry;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to manage custom game rules.
 * Usage: /customgamerule <rule> [value] [world]
 */
public class GameRuleCommand extends SimpleCommand {
    private final BukkitPlatform platform;
    
    public GameRuleCommand(@NotNull BukkitPlatform platform) {
        super("customgamerule", "hollow.gamerule");
        this.platform = platform;
        this.setDescription("Manage custom game rules");
        this.setUsage("/customgamerule [rule] [value] [world]");
    }
    
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var args = ctx.getArgs();
        final var sender = ctx.getSender();
        if (args.length == 0) {
            StringUtils.send(sender, "<gold>=== Custom Game Rules ===");
            final var rules = GameRuleRegistry.getAllGameRules();
            if (rules.isEmpty()) {
                StringUtils.send(sender, "<gray>No custom game rules registered.");
                return;
            }
            for (CustomGameRule<?> rule : rules) {
                String scope = rule.global() ? " <aqua>[GLOBAL]" : "";
                StringUtils.send(sender, "<yellow>" + rule.name() + scope + " <gray>(" + rule.type().getSimpleName() + ")<white> - Default: <green>" + rule.defaultValue());
                if (!rule.description().isEmpty()) {
                    StringUtils.send(sender, "<gray>  " + rule.description());
                }
            }
            return;
        }
        
        final String ruleName = args[0];
        final CustomGameRule<?> gameRule = GameRuleRegistry.getGameRule(ruleName);
        
        if (gameRule == null) {
            throw new CommandFailedException("§cGamerule desconhecida: " + ruleName + "\n" + "§7Use /customgamerule para ver as gamerules disponíveis.");
        }
        
        World targetWorld = null;
        if (gameRule.global()) {
            if (args.length >= 3) {
                StringUtils.send(sender, "<yellow>Warning: <gray>Ignorando parâmetro de mundo para rule global  <white>'" + ruleName + "'</white>.");
            }
        } else {
            if (args.length >= 3) {
                targetWorld = Bukkit.getWorld(args[2]);
                if (targetWorld == null) {
                    throw new CommandFailedException("§cMundo desconhecido: " + args[2]);
                }
            } else if (sender instanceof Player player) {
                targetWorld = player.getWorld();
            } else {
                targetWorld = Bukkit.getWorlds().getFirst();
            }
        }
        
        if (args.length == 1) {
            @SuppressWarnings("unchecked")
            final CustomGameRule<Object> typedRule = (CustomGameRule<Object>) gameRule;
            final Object value;
            
            if (gameRule.global()) {
                value = platform.getGameRuleManager().getGlobalGameRule(typedRule);
                StringUtils.send(sender, "<yellow>" + ruleName + "<aqua> [GLOBAL] <gray>= <green>" + value);
            } else {
                value = platform.getGameRuleManager().getWorldGameRule(targetWorld, typedRule);
                StringUtils.send(sender, "<yellow>" + ruleName + "<gray> = <green>" + value + "<gray> (in " + targetWorld.getName() + ")");
            }
            return;
        }
        
        final String valueStr = args[1];
        try {
            @SuppressWarnings("unchecked")
            final CustomGameRule<Object> typedRule = (CustomGameRule<Object>) gameRule;
            final Object parsedValue = gameRule.parseValue(valueStr);
            if (parsedValue instanceof Location loc) {
                loc.setWorld(targetWorld);
            }
            
            if (gameRule.global()) {
                platform.getGameRuleManager().setGlobalGameRule(typedRule, parsedValue);
                StringUtils.send(sender, "<green>Set " + "<yellow>" + ruleName + "<aqua> [GLOBAL] " + "<green>to " + "<white>" + parsedValue);
            } else {
                platform.getGameRuleManager().setWorldGameRule(targetWorld, typedRule, parsedValue);
                StringUtils.send(sender, "<green>Set " + "<yellow>" + ruleName + "<green> to " + "<white>" + parsedValue + "<green> in world " + "<yellow>" + targetWorld.getName());
            }
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException("§cInvalid value: " + e.getMessage() + "\n§7Expected type: " + gameRule.type().getSimpleName());
        }
    }
    
    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();
        
        if (args.length == 1) {
            return filterStartingWith(GameRuleRegistry.getAllGameRules().stream().map(CustomGameRule::name).collect(Collectors.toList()), args[0]);
        } else if (args.length == 2) {
            final CustomGameRule<?> rule = GameRuleRegistry.getGameRule(args[0]);
            if (rule != null) {
                if (rule.type() == Boolean.class) {
                    return filterStartingWith(List.of("true", "false"), args[1]);
                }
                else if (rule.type().isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) rule.type();
                    List<String> enumNames = new ArrayList<>();
                    for (Enum<?> anEnum : enumType.getEnumConstants()) {
                        String name = anEnum.name();
                        enumNames.add(name);
                    }
                    return filterStartingWith(enumNames, args[1]);
                }
                else if (rule.type() == Location.class) {
                    return List.of("<x,y,z>");
                }
            }
            return NONE_ARGS;
        } else if (args.length == 3) {
            return filterStartingWith(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[2]);
        }
        
        return NONE_ARGS;
    }
}

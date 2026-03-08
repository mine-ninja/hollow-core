package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TpCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpCommand(@NotNull EssentialsPlugin plugin) {
        super("tp", "hollow.tp");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player sender = ctx.getSenderAsPlayer();
        String[] args = ctx.getArgs();

        if (args.length == 0) {
            throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tp <player> [target] | /tp <x> <y> <z>"));
        }

        if (args.length == 1) {
            // /tp <player> - teleport self to player
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                msg().send(sender, "player-not-found", "player", targetName);
                return;
            }

            plugin.getTeleportService().teleport(sender, target.getLocation());
            msg().send(sender, "tp-teleported", "target", target.getName());
            return;
        }

        if (args.length == 2) {
            // /tp <player> <target> - teleport player to target (admin)
            if (!sender.hasPermission("hollow.tp.others")) {
                msg().send(sender, "no-permission");
                return;
            }

            String playerName = args[0];
            String targetName = args[1];

            Player player = Bukkit.getPlayer(playerName);
            Player target = Bukkit.getPlayer(targetName);

            if (player == null) {
                msg().send(sender, "player-not-found", "player", playerName);
                return;
            }
            if (target == null) {
                msg().send(sender, "player-not-found", "player", targetName);
                return;
            }

            plugin.getTeleportService().teleport(player, target.getLocation());
            msg().send(sender, "tp-teleported", "target", target.getName());
            return;
        }

        if (args.length == 3) {
            // /tp <x> <y> <z> - teleport self to coordinates in current world
            if (!isNumeric(args[0]) || !isNumeric(args[1]) || !isNumeric(args[2])) {
                throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tp <player> [target] | /tp <x> <y> <z>"));
            }

            Location current = sender.getLocation();
            Location destination = new Location(
                sender.getWorld(),
                Double.parseDouble(args[0]),
                Double.parseDouble(args[1]),
                Double.parseDouble(args[2]),
                current.getYaw(),
                current.getPitch()
            );

            plugin.getTeleportService().teleport(sender, destination);
            msg().send(sender, "tp-teleported", "target", args[0] + " " + args[1] + " " + args[2]);
            return;
        }

        throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tp <player> [target] | /tp <x> <y> <z>"));
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        Player sender = ctx.getSenderAsPlayer();
        String[] args = ctx.getArgs();

        if (args.length == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .forEach(suggestions::add);

            String x = String.valueOf(sender.getLocation().getBlockX());
            if (x.startsWith(args[0])) {
                suggestions.add(x);
            }
            return suggestions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();

            if (sender.hasPermission("hollow.tp.others") && Bukkit.getPlayer(args[0]) != null) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
            }

            if (isNumeric(args[0])) {
                String y = String.valueOf(sender.getLocation().getBlockY());
                return y.startsWith(args[1]) ? List.of(y) : List.of();
            }

            return List.of();
        }

        if (args.length == 3 && isNumeric(args[0]) && isNumeric(args[1])) {
            String z = String.valueOf(sender.getLocation().getBlockZ());
            return z.startsWith(args[2]) ? List.of(z) : List.of();
        }

        return List.of();
    }

    private boolean isNumeric(@NotNull String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}

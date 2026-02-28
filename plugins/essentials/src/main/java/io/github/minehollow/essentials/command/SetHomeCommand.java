package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public SetHomeCommand(@NotNull EssentialsPlugin plugin) {
        super("sethome", "hollow.sethome");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        String name = ctx.getRawArgOrNull(0);
        if (name == null) {
            name = "home";
        }

        final String homeName = name;
        int limit = resolveHomeLimit(player);

        Thread.startVirtualThread(() -> {
            boolean success = plugin.getHomeService().setHome(
                player.getUniqueId(), homeName, player.getLocation(), limit
            );

            if (success) {
                msg().send(player, "home-set", "name", homeName);
            } else {
                msg().send(player, "home-limit-reached", "limit", String.valueOf(limit));
            }
        });
    }


    /**
     * Resolves the highest hollow.homes.N permission the player has. Falls back to config default-limit if none found.
     */
    private int resolveHomeLimit(@NotNull Player player) {
        int max = plugin.getConfig().getInt("homes.default-limit", 1);

        final var permissionPrefix = "hollow.homes.";
        for (int i = 1; i <= 50; i++) { // Arbitrary upper limit to prevent infinite loop
            if (player.hasPermission(permissionPrefix + i)) {
                max = i;
            } else {
                break; // Assuming permissions are sequential, we can stop checking after the first miss
            }
        }

        return max;
    }
}


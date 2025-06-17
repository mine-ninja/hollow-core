package net.warcane.lugin.core.minecraft.command.context;

import lombok.Data;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Data
public class CommandContext {

    private final CommandSender sender;
    private final String[] args;


    public @Nullable Player getSenderAsPlayer() {
        if (sender instanceof Player player) {
            return player;
        }
        return null;
    }

    public <R> R getArgOrThrow(int argIndex, @NotNull Function<String, R> function, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return function.apply(args[argIndex]);
        } catch (Exception e) {
            throw new CommandFailedException(errorMessage);
        }
    }

    public <R> R getArgOrDefault(int argIndex, @NotNull Function<String, R> function, @Nullable R defaultValue) {
        if (argIndex < 0 || argIndex >= args.length) {
            return defaultValue;
        }
        try {
            return function.apply(args[argIndex]);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getIntOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return Integer.parseInt(args[argIndex]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException(errorMessage);
        }
    }

    public int getIntOrDefault(int argIndex, int defaultValue) {
        if (argIndex < 0 || argIndex >= args.length) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[argIndex]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDoubleOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return Double.parseDouble(args[argIndex]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException(errorMessage);
        }
    }

    public double getDoubleOrDefault(int argIndex, double defaultValue) {
        if (argIndex < 0 || argIndex >= args.length) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(args[argIndex]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Player getLocalPlayerOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (args.length <= argIndex) {
            throw new CommandFailedException(errorMessage);
        }

        Player player = Bukkit.getPlayer(args[argIndex]);
        if (player == null) {
            throw new CommandFailedException(errorMessage);
        }

        if (!player.isOnline()) {
            throw new CommandFailedException("§cO jogador " + player.getName() + " não está online.");
        }

        return player;
    }

    // for enum
    public <E extends Enum<E>> E getEnumOrThrow(int argIndex, @NotNull Class<E> enumClass, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return Enum.valueOf(enumClass, args[argIndex].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException(errorMessage);
        }
    }
}

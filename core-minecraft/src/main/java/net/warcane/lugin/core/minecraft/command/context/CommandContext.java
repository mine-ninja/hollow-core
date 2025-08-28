package net.warcane.lugin.core.minecraft.command.context;

import lombok.Data;

import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.util.time.Time;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

@Data
public class CommandContext {

    private static final Pattern BIG_INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern BIG_DECIMAL_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final CommandSender sender;
    private final String[] args;

    public boolean isArgsLength(int length) {
        return args.length == length;
    }

    public void sendMessage(@NotNull String message) {
        sender.sendMessage(message);
    }

    public void sendMessage(@NotNull String... message) {
        for (String msg : message) {
            sender.sendMessage(msg);
        }
    }
    
    public void sendMessage(@NotNull Component... message) {
        for (Component msg : message) {
            sender.sendMessage(msg);
        }
    }

    public Player getSenderAsPlayer() {
        if (sender instanceof Player player) {
            return player;
        }
        throw new CommandFailedException("§cNão é possível executar este comando, pois o executor não é um jogador.");
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

    public String getRawArgOrNull(int argIndex) {
        if (argIndex < 0 || argIndex >= args.length) {
            return null;
        }
        return args[argIndex];
    }

    public String getRawArgOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        return args[argIndex];
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

    public BigInteger getBigIntegerOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        if (!BIG_INTEGER_PATTERN.matcher(args[argIndex]).matches()) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return new BigInteger(args[argIndex]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException(errorMessage);
        }
    }

    public BigDecimal getBigDecimalOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        if (!BIG_DECIMAL_PATTERN.matcher(args[argIndex]).matches()) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return new BigDecimal(args[argIndex]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException(errorMessage);
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

    public <T> T throwIfNull(@Nullable T obj, @NotNull String errorMsg) {
        if (obj == null) {
            throw new CommandFailedException(errorMsg);
        }
        return obj;
    }

    public Time getTimeOrThrow(int argIndex, @NotNull String errorMessage) throws CommandFailedException {
        if (argIndex < 0 || argIndex >= args.length) {
            throw new CommandFailedException(errorMessage);
        }
        try {
            return Time.parseString(args[argIndex]);
        } catch (Exception e) {
            throw new CommandFailedException(errorMessage);
        }
    }

    public String joinArgs(int startIndex) {
        if (startIndex < 0 || startIndex >= args.length) {
            throw new CommandFailedException("§cÍndice de início inválido para junção de argumentos.");
        }

        return StringUtils.join(args, " ", startIndex, args.length);
    }
}

package io.github.minehollow.minecraft.internal.command.gamemode;

import com.google.common.collect.ImmutableMap;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GameModeCommand extends SimpleCommand {
    private static final ImmutableMap<String, GameMode> GAMEMODE_ALIASES = ImmutableMap.<String, GameMode>builder()
      .put("survival", GameMode.SURVIVAL)
      .put("s", GameMode.SURVIVAL)
      .put("0", GameMode.SURVIVAL)
      .put("creative", GameMode.CREATIVE)
      .put("c", GameMode.CREATIVE)
      .put("1", GameMode.CREATIVE)
      .put("adventure", GameMode.ADVENTURE)
      .put("a", GameMode.ADVENTURE)
      .put("2", GameMode.ADVENTURE)
      .put("spectator", GameMode.SPECTATOR)
      .put("sp", GameMode.SPECTATOR)
      .put("3", GameMode.SPECTATOR)
      .build();

    private final BukkitPlatform platform;

    public GameModeCommand(@NotNull BukkitPlatform platform) {
        super("gamemode", "hollow.gamemode");
        setAliases(List.of("gm"));
        this.platform = platform;
    }

    private MessageConfig messages() {
        return platform.getMessageConfig();
    }

    private String rawMsg(String key, Object... replacements) {
        return messages().getRaw("gamemode-messages", key, replacements);
    }


    private void sendMsg(CommandContext ctx, String key, Object... replacements) {
        ctx.sendMessage(messages().get("gamemode-messages", key, replacements));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        final var gameModeArg = ctx.getArgOrThrow(0, this::parseGameMode, rawMsg("invalid-usage"));
        final var permissionNodeForThisGameMode = switch (gameModeArg) {
            case SURVIVAL -> "hollow.gamemode.survival";
            case CREATIVE -> "hollow.gamemode.creative";
            case ADVENTURE -> "hollow.gamemode.adventure";
            case SPECTATOR -> "hollow.gamemode.spectator";
        };
        if (!player.hasPermission(permissionNodeForThisGameMode)) {
            throw new CommandFailedException(rawMsg("no-permission"));
        }
        final var targetOrNull = ctx.getRawArgOrNull(1);
        if (targetOrNull == null) {
            setPlayerGameMode(player, gameModeArg);
            sendMsg(ctx, "self-changed", "gamemode", getGameModeName(gameModeArg));
        } else {
            final var targetPlayer = Bukkit.getOnlinePlayers().stream()
              .filter(p -> p.getName().equalsIgnoreCase(targetOrNull))
              .findFirst()
              .orElse(null);
            if (targetPlayer == null) {
                throw new CommandFailedException(rawMsg("player-not-found", "target", targetOrNull));
            }
            setPlayerGameMode(targetPlayer, gameModeArg);
            sendMsg(ctx, "other-changed", "target", targetPlayer.getName(), "gamemode", getGameModeName(gameModeArg));
            if (!targetPlayer.equals(player)) {
                messages().send(targetPlayer, "gamemode-messages", "changed-by", "gamemode", getGameModeName(gameModeArg), "player", player.getName());
            }
        }
    }

    private void setPlayerGameMode(Player player, GameMode gameMode) {
        player.setGameMode(gameMode);
    }

    private GameMode parseGameMode(String input) {
        return GAMEMODE_ALIASES.get(input.toLowerCase());
    }

    private String getGameModeName(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> "Sobrevivência";
            case CREATIVE -> "Criativo";
            case ADVENTURE -> "Aventura";
            case SPECTATOR -> "Espectador";
        };
    }
}

package io.github.minehollow.minecraft.internal.command.gamemode;

import com.google.common.collect.ImmutableMap;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
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


    public GameModeCommand() {
        super("gamemode", "hollow.gamemode");
        setAliases(List.of("gm"));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        final var gameModeArg = ctx.getArgOrThrow(0, this::parseGameMode, "Modo de jogo inválido. Use: survival, creative, adventure, spectator.");
        final var permissionNodeForThisGameMode = switch (gameModeArg) {
            case SURVIVAL -> "hollow.gamemode.survival";
            case CREATIVE -> "hollow.gamemode.creative";
            case ADVENTURE -> "hollow.gamemode.adventure";
            case SPECTATOR -> "hollow.gamemode.spectator";
        };

        if (!player.hasPermission(permissionNodeForThisGameMode)) {
            throw new CommandFailedException("Você não tem para usar este comando");
        }

        final var targetOrNull = ctx.getRawArgOrNull(1);
        if (targetOrNull == null) {
            setPlayerGameMode(player, gameModeArg);
            player.sendMessage("§aSeu modo de jogo foi alterado para §e" + getGameModeName(gameModeArg) + "§a.");
        } else {
            final var targetPlayer = Bukkit.getOnlinePlayers().stream()
              .filter(p -> p.getName().equalsIgnoreCase(targetOrNull))
              .findFirst()
              .orElse(null);

            if (targetPlayer == null) {
                throw new CommandFailedException("Jogador não encontrado: " + targetOrNull);
            }

            setPlayerGameMode(targetPlayer, gameModeArg);
            player.sendMessage("§aModo de jogo de §e" + targetPlayer.getName() + "§a alterado para §e" + getGameModeName(gameModeArg) + "§a.");
            if (!targetPlayer.equals(player)) {
                targetPlayer.sendMessage("§aSeu modo de jogo foi alterado para §e" + getGameModeName(gameModeArg) + "§a por §e" + player.getName() + "§a.");
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

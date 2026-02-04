package io.github.minehollow.mines.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.filler.MineFiller;
import io.github.minehollow.mines.model.Mine;
import io.github.minehollow.mines.util.MinePreSelection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MinesCommand extends SimpleCommand {

    private final MinesPlugin plugin;

    public MinesCommand(@NotNull MinesPlugin plugin) {
        super("mina");
        setAliases(List.of("mines", "mine"));
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            // abrir menu principal de minas
            return;
        }


        if (!ctx.getSender().hasPermission("mines.admin")) {
            throw new CommandFailedException("Você não tem permissão para usar este comando.");
        }

        if (subCommand.equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getMineManager().initializeMines();
            ctx.sendMessage("§aConfiguração recarregada com sucesso.");
        }

        if (subCommand.equalsIgnoreCase("reset")) {
            final var mineId = ctx.getRawArgOrThrow(1, "Você deve especificar o ID da mina para resetar.");
            final var mine = plugin.getMineManager().getMineById(mineId);
            if (mine == null) {
                throw new CommandFailedException("Mina com ID '" + mineId + "' não encontrada.");
            }

            MineFiller.fillMine(plugin.getMineManager(), mine, () -> {
                ctx.sendMessage("§aMina '" + mineId + "' resetada com sucesso.");
            });
        }

        if (subCommand.equalsIgnoreCase("list")) {
            final var asList = plugin.getMineManager().getMines()
              .values()
              .stream()
              .map(Mine::toStatistics)
              .toList();

            ctx.sendMessage("§eMinas disponíveis:");
            for (final var stats : asList) {
                final var id = stats.id();
                final var chunkCount = stats.chunkCount();
                ctx.sendMessage("§7- §fID: §a" + id + " §f| Chunks: §a" + chunkCount);
            }
        }

        if (subCommand.equalsIgnoreCase("create")) {
            if (!(ctx.getSender() instanceof Player player)) {
                throw new CommandFailedException("Apenas jogadores podem usar este comando.");
            }

            final var mineId = ctx.getRawArgOrThrow(1, "Você deve especificar o ID da mina a ser criada.");
            final var selection = MinePreSelection.getCurrent(player);
            if (selection == null) {
                throw new CommandFailedException("Você deve fazer uma pré-seleção da área da mina primeiro.");
            }

            final var newMine = Mine.createDefaultMine(mineId, player, selection);
            plugin.getMineManager().addAndSaveMine(mineId, newMine);
        }

        if (subCommand.equalsIgnoreCase("setMaxY")) {
            final var mineId = ctx.getRawArgOrThrow(1, "Você deve especificar o ID da mina.");
            final var mine = plugin.getMineManager().getMineById(mineId);
            if (mine == null) {
                throw new CommandFailedException("Mina com ID '" + mineId + "' não encontrada.");
            }

            final int maxY = ctx.getIntOrThrow(2, "Você deve especificar a altura máxima (Y) para a mina.");
            mine.setMaxY(maxY);
            plugin.getMineManager().addAndSaveMine(mineId, mine);
        }

        if (subCommand.equalsIgnoreCase("setMinY")) {
            final var mineId = ctx.getRawArgOrThrow(1, "Você deve especificar o ID da mina.");
            final var mine = plugin.getMineManager().getMineById(mineId);
            if (mine == null) {
                throw new CommandFailedException("Mina com ID '" + mineId + "' não encontrada.");
            }

            final int minY = ctx.getIntOrThrow(2, "Você deve especificar a altura mínima (Y) para a mina.");
            mine.setMinY(minY);
            plugin.getMineManager().addAndSaveMine(mineId, mine);
        }

        if (subCommand.equalsIgnoreCase("setWorld")) {
            final var newWorldName = ctx.getRawArgOrThrow(1, "Você deve especificar o nome do mundo.");
            final var bukkitWorld = plugin.getServer().getWorld(newWorldName);
            if (bukkitWorld == null) {
                throw new CommandFailedException("Mundo com nome '" + newWorldName + "' não encontrado.");
            }

            plugin.getMineManager().setMineWorldName(newWorldName);
            ctx.sendMessage("§aMundo das minas definido para '" + newWorldName + "'.");
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();
        if (args.length == 1) {
            return List.of("reload", "reset", "create");
        }

        final var subCommand = args[0];
        if (subCommand.equalsIgnoreCase("reset")) {
            if (args.length == 2) {
                return plugin.getMineManager().getMines()
                  .values()
                  .stream()
                  .map(Mine::getId)
                  .toList();
            }
        }

        if (subCommand.equalsIgnoreCase("create")) {
            if (args.length == 2) {
                return List.of("<nova_mina_id>");
            }
        }

        return List.of();
    }
}

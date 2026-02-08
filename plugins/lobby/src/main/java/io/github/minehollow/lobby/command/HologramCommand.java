package io.github.minehollow.lobby.command;

import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class HologramCommand extends SimpleCommand {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
      "create", "remove", "teleport", "setlines", "addline", "removeline", "list"
    );

    private final HologramManager hologramManager;

    public HologramCommand(@NotNull HologramManager hologramManager) {
        super("hologram", "hologram.admin");
        this.hologramManager = hologramManager;

        setDescription("Gerencia hologramas do lobby");
        setUsage("/hologram <create|remove|teleport|setlines|addline|removeline|list>");
        setAliases(Arrays.asList("holo", "hg"));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var subCommand = ctx.getRawArgOrThrow(0, "<red>Use: /hologram <create|remove|teleport|setlines|addline|removeline|list>");

        switch (subCommand.toLowerCase()) {
            case "create" -> handleCreate(ctx);
            case "remove", "delete" -> handleRemove(ctx);
            case "teleport", "tp" -> handleTeleport(ctx);
            case "setlines" -> handleSetLines(ctx);
            case "addline" -> handleAddLine(ctx);
            case "removeline" -> handleRemoveLine(ctx);
            case "list" -> handleList(ctx);
            default -> StringUtils.send(sender, "<red>Subcomando desconhecido. Use: <yellow>/hologram list <red>para ver os comandos disponíveis.");
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        var args = ctx.getArgs();

        // Subcomando principal
        if (args.length == 1) {
            return filterStartingWith(SUBCOMMANDS, args[0]);
        }

        // Segundo argumento (ID do holograma)
        if (args.length == 2) {
            var subCmd = args[0].toLowerCase();

            // Comandos que precisam de ID de holograma existente
            if (List.of("remove", "delete", "teleport", "tp", "setlines", "addline", "removeline").contains(subCmd)) {
                return filterStartingWith(getHologramIds(), args[1]);
            }
        }

        return NONE_ARGS;
    }

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePlayer(ctx);
        var player = (Player) ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique um ID para o holograma. <gray>Exemplo: /hologram create holo1 <text>");

        if (hologramManager.getHologram(id) != null) {
            throw new CommandFailedException("<red>Já existe um holograma com o ID '<yellow>" + id + "<red>'.");
        }

        var args = ctx.getArgs();
        if (args.length < 3) {
            throw new CommandFailedException("<red>Especifique as linhas do holograma.\n<gray>Use <yellow>| <gray>para separar linhas.\n<yellow>Exemplo: /hologram create holo1 <green>Linha 1<yellow>|<blue>Linha 2");
        }

        var textParts = Arrays.copyOfRange(args, 2, args.length);
        var hologramText = String.join(" ", textParts);
        var lines = Arrays.asList(hologramText.split("\\|"));

        hologramManager.createHologram(id, player.getLocation(), lines);

        StringUtils.send(player, "<green>✓ Holograma '<yellow>" + id + "<green>' criado com sucesso!");
        StringUtils.send(player, "<gray>Linhas criadas: <white>" + lines.size());
        StringUtils.send(player, "<gray>Dica: Use <white>/hologram setlines " + id + " <text> <gray>para editar.");
    }

    private void handleRemove(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do holograma. <gray>Use /hologram list para ver os hologramas.");

        if (!hologramManager.removeHologram(id)) {
            throw new CommandFailedException("<red>Holograma '<yellow>" + id + "<red>' não encontrado.");
        }

        StringUtils.send(sender, "<green>✓ Holograma '<yellow>" + id + "<green>' removido com sucesso!");
    }

    private void handleTeleport(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePlayer(ctx);
        var player = (Player) ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do holograma.");

        if (!hologramManager.teleportHologram(id, player.getLocation())) {
            throw new CommandFailedException("<red>Holograma '<yellow>" + id + "<red>' não encontrado.");
        }

        StringUtils.send(player, "<green>✓ Holograma '<yellow>" + id + "<green>' teleportado para sua localização!");
    }

    private void handleSetLines(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do holograma.");

        var args = ctx.getArgs();
        if (args.length < 3) {
            throw new CommandFailedException("<red>Especifique as novas linhas do holograma.\n<gray>Use <yellow>| <gray>para separar linhas.\n<yellow>Exemplo: /hologram setlines holo1 <green>Nova linha 1<yellow>|<blue>Nova linha 2");
        }

        var textParts = Arrays.copyOfRange(args, 2, args.length);
        var hologramText = String.join(" ", textParts);
        var lines = Arrays.asList(hologramText.split("\\|"));

        if (!hologramManager.updateHologramLines(id, lines)) {
            throw new CommandFailedException("<red>Holograma '<yellow>" + id + "<red>' não encontrado.");
        }

        StringUtils.send(sender, "<green>✓ Linhas do holograma '<yellow>" + id + "<green>' atualizadas!");
        StringUtils.send(sender, "<gray>Total de linhas: <white>" + lines.size());
    }

    private void handleAddLine(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do holograma.");

        var args = ctx.getArgs();
        if (args.length < 3) {
            throw new CommandFailedException("<red>Especifique a linha a ser adicionada.\n<yellow>Exemplo: /hologram addline holo1 <green>Nova linha");
        }

        var handler = hologramManager.getHologram(id);
        if (handler == null) {
            throw new CommandFailedException("<red>Holograma '<yellow>" + id + "<red>' não encontrado.");
        }

        var textParts = Arrays.copyOfRange(args, 2, args.length);
        var newLine = String.join(" ", textParts);

        var currentLines = handler.getData().lines();
        var updatedLines = new java.util.ArrayList<>(currentLines);
        updatedLines.add(newLine);

        hologramManager.updateHologramLines(id, updatedLines);

        StringUtils.send(sender, "<green>✓ Linha adicionada ao holograma '<yellow>" + id + "<green>'!");
        StringUtils.send(sender, "<gray>Total de linhas: <white>" + updatedLines.size());
    }

    private void handleRemoveLine(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do holograma.");

        var lineIndexStr = ctx.getRawArgOrThrow(2, "<red>Especifique o índice da linha a ser removida (começando em 1).\n<yellow>Exemplo: /hologram removeline holo1 1");

        var handler = hologramManager.getHologram(id);
        if (handler == null) {
            throw new CommandFailedException("<red>Holograma '<yellow>" + id + "<red>' não encontrado.");
        }

        int lineIndex;
        try {
            lineIndex = Integer.parseInt(lineIndexStr) - 1; // Usuário usa índice baseado em 1
        } catch (NumberFormatException e) {
            throw new CommandFailedException("<red>Índice inválido. Use um número válido.");
        }

        var currentLines = handler.getData().lines();

        if (lineIndex < 0 || lineIndex >= currentLines.size()) {
            throw new CommandFailedException("<red>Índice fora do intervalo. Este holograma tem <yellow>" + currentLines.size() + "<red> linha(s).");
        }

        var updatedLines = new java.util.ArrayList<>(currentLines);
        updatedLines.remove(lineIndex);

        if (updatedLines.isEmpty()) {
            throw new CommandFailedException("<red>Não é possível remover a última linha. Use <yellow>/hologram remove " + id + "<red> para deletar o holograma.");
        }

        hologramManager.updateHologramLines(id, updatedLines);

        StringUtils.send(sender, "<green>✓ Linha removida do holograma '<yellow>" + id + "<green>'!");
        StringUtils.send(sender, "<gray>Total de linhas restantes: <white>" + updatedLines.size());
    }

    private void handleList(@NotNull CommandContext ctx) {
        var sender = ctx.getSender();
        var holograms = hologramManager.getAllHolograms();

        if (holograms.isEmpty()) {
            StringUtils.send(sender, "<yellow>Nenhum holograma foi criado ainda.");
            StringUtils.send(sender, "<gray>Use <white>/hologram create <id> <text> <gray>para criar um.");
            return;
        }

        StringUtils.send(sender, "<green><bold>Hologramas criados <gray>(" + holograms.size() + "):");
        holograms.forEach(handler -> {
            var data = handler.getData();
            var loc = data.location();
            var world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

            StringUtils.send(sender, " <yellow>• " + data.id());
            StringUtils.send(sender, "   <gray>Mundo: <white>" + world + " <gray>X: <white>" + (int)loc.getX()
                                     + " <gray>Y: <white>" + (int)loc.getY() + " <gray>Z: <white>" + (int)loc.getZ());
            StringUtils.send(sender, "   <gray>Linhas: <white>" + data.lines().size());

            // Mostra preview das primeiras 2 linhas
            int lineCount = Math.min(2, data.lines().size());
            for (int i = 0; i < lineCount; i++) {
                StringUtils.send(sender, "   <gray>" + (i + 1) + ". <white>" + data.lines().get(i));
            }

            if (data.lines().size() > 2) {
                StringUtils.send(sender, "   <gray>... e mais " + (data.lines().size() - 2) + " linha(s)");
            }
        });
    }

    private void requirePlayer(@NotNull CommandContext ctx) throws CommandFailedException {
        if (!(ctx.getSender() instanceof Player)) {
            throw new CommandFailedException("<red>Apenas jogadores podem executar este comando.");
        }
    }

    private List<String> getHologramIds() {
        return hologramManager.getAllHolograms().stream()
          .map(handler -> handler.getData().id())
          .collect(Collectors.toList());
    }
}
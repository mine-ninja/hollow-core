package io.github.minehollow.lobby.command;

import io.github.minehollow.lobby.npc.NPCManager;
import io.github.minehollow.lobby.service.SkinService;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class NPCCommand extends SimpleCommand {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
      "create", "remove", "setskin", "teleport", "attach", "detach", "setinteraction", "list", "reload"
      "create", "remove", "setskin", "teleport", "attach", "detach", "setinteraction", "list", "reload"
    );

    private final NPCManager npcManager;
    private final SkinService skinService;

    public NPCCommand(@NotNull NPCManager npcManager, @NotNull SkinService skinService) {
        super("npc", "npc.admin");
        this.npcManager = npcManager;
        this.skinService = skinService;

        setDescription("Gerencia NPCs do lobby");
        setUsage("/npc <create|remove|setskin|teleport|attach|detach|setinteraction|list|reload>");
        setUsage("/npc <create|remove|setskin|teleport|attach|detach|setinteraction|list|reload>");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var subCommand = ctx.getRawArgOrThrow(0, "<red>Use: /npc <create|remove|setskin|teleport|attach|detach|setinteraction|list|reload>");
        var subCommand = ctx.getRawArgOrThrow(0, "<red>Use: /npc <create|remove|setskin|teleport|attach|detach|setinteraction|list|reload>");

        switch (subCommand.toLowerCase()) {
            case "create" -> handleCreate(ctx);
            case "remove" -> handleRemove(ctx);
            case "setskin" -> handleSetSkin(ctx);
            case "teleport", "tp" -> handleTeleport(ctx);
            case "attach" -> handleAttachHologram(ctx);
            case "detach" -> handleDetachHologram(ctx);
            case "setinteraction" -> handleSetInteraction(ctx);
            case "list" -> handleList(ctx);
            case "reload" -> handleReload(ctx);
            default ->
              StringUtils.send(sender, "<red>Subcomando desconhecido. Use: <yellow>/npc list <red>para ver os comandos disponíveis.");
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        var args = ctx.getArgs();

        // Subcomando principal

        // Subcomando principal
        if (args.length == 1) {
            return filterStartingWith(SUBCOMMANDS, args[0]);
        }

        // Segundo argumento (ID do NPC)

        // Segundo argumento (ID do NPC)
        if (args.length == 2) {
            var subCmd = args[0].toLowerCase();

            // Comandos que precisam de ID de NPC existente

            // Comandos que precisam de ID de NPC existente
            if (List.of("remove", "setskin", "teleport", "tp", "attach", "detach", "setinteraction").contains(subCmd)) {
                return filterStartingWith(getNPCIds(), args[1]);
            }
        }

        // Terceiro argumento

        // Terceiro argumento
        if (args.length == 3) {
            var subCmd = args[0].toLowerCase();

            // attach precisa de ID de holograma

            // attach precisa de ID de holograma
            if (subCmd.equals("attach")) {
                return filterStartingWith(getHologramIds(), args[2]);
            }

            // setskin pode aceitar nome de jogador online

            // setskin pode aceitar nome de jogador online
            if (subCmd.equals("setskin")) {
                return filterStartingWith(getOnlinePlayerNames(), args[2]);
            }
        }


        return NONE_ARGS;
    }

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePlayer(ctx);
        var player = (Player) ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique um ID para o NPC. <gray>Exemplo: /npc create npc1");

        if (npcManager.getNPC(id) != null) {
            throw new CommandFailedException("<red>Já existe um NPC com o ID '<yellow>" + id + "<red>'.");
        }

        npcManager.createNPC(id, player.getLocation());
        StringUtils.send(player, "<green>✓ NPC '<yellow>" + id + "<green>' criado com sucesso!");
        StringUtils.send(player, "<gray>Dica: Use <white>/npc setskin " + id + " <jogador> <gray>para alterar a aparência.");
    }

    private void handleRemove(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var id = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC. <gray>Use /npc list para ver os NPCs.");

        if (!npcManager.removeNPC(id)) {
            throw new CommandFailedException("<red>NPC '<yellow>" + id + "<red>' não encontrado.");
        }

        StringUtils.send(sender, "<green>✓ NPC '<yellow>" + id + "<green>' removido com sucesso!");
    }

    private void handleSetSkin(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var npcId = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC.");
        var handler = npcManager.getNPC(npcId);

        if (handler == null) {
            throw new CommandFailedException("<red>NPC '<yellow>" + npcId + "<red>' não encontrado.");
        }

        // Se forneceu um argumento (Nome ou URL)
        if (ctx.getArgs().length >= 3) {
            String input = ctx.getRawArgOrThrow(2, "<red>Informe o nome ou URL da skin.");
            StringUtils.send(sender, "<gray>Processando skin para <yellow>" + input + "<gray>...");

            CompletableFuture<SkinService.SkinData> future = input.startsWith("http")
              ? skinService.fetchSkinByURL(input)
              : skinService.fetchSkinByName(input);

            future.thenAccept(skinData -> {
                if (skinData == null) {
                    StringUtils.send(sender, "<red>Erro ao obter skin de: " + input);
                    return;
                }

                Tasks.runSync(() -> {
                    npcManager.setSkinByData(npcId, skinData.texture(), skinData.signature());
                    StringUtils.send(sender, "<green>✓ Skin do NPC atualizada com sucesso!");
                });
            });
            return;
        }

        // Comportamento padrão: usa a skin do executor
        requirePlayer(ctx);
        if (npcManager.setSkin(npcId, (Player) sender)) {
            StringUtils.send(sender, "<green>✓ Skin do NPC atualizada para a sua!");
        }
    }

    private void handleTeleport(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePlayer(ctx);
        var player = (Player) ctx.getSender();
        var npcId = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC.");

        if (!npcManager.teleportNPC(npcId, player.getLocation())) {
            throw new CommandFailedException("<red>NPC '<yellow>" + npcId + "<red>' não encontrado.");
        }

        StringUtils.send(player, "<green>✓ NPC '<yellow>" + npcId + "<green>' teleportado para sua localização!");
    }

    private void handleAttachHologram(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var npcId = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC.");
        var hologramId = ctx.getRawArgOrThrow(2, "<red>Especifique o ID do holograma.");

        if (!npcManager.attachHologram(npcId, hologramId)) {
            throw new CommandFailedException("<red>NPC ou holograma não encontrado.");
        }

        StringUtils.send(sender, "<green>✓ Holograma '<yellow>" + hologramId + "<green>' anexado ao NPC '<yellow>" + npcId + "<green>'!");
    }

    private void handleDetachHologram(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var npcId = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC.");

        if (!npcManager.detachHologram(npcId)) {
            throw new CommandFailedException("<red>NPC '<yellow>" + npcId + "<red>' não encontrado ou não possui holograma anexado.");
        }

        StringUtils.send(sender, "<green>✓ Holograma desanexado do NPC '<yellow>" + npcId + "<green>'!");
    }

    private void handleSetInteraction(@NotNull CommandContext ctx) throws CommandFailedException {
        var sender = ctx.getSender();
        var npcId = ctx.getRawArgOrThrow(1, "<red>Especifique o ID do NPC.");
        var interaction = ctx.getRawArgOrThrow(2, "<red>Especifique a interação. <gray>Exemplos: server:lobby, command:give {player} diamond");

        if (!npcManager.setInteraction(npcId, interaction)) {
            throw new CommandFailedException("<red>NPC '<yellow>" + npcId + "<red>' não encontrado.");
        }

        StringUtils.send(sender, "<green>✓ Interação do NPC '<yellow>" + npcId + "<green>' definida!");
        StringUtils.send(sender, "<gray>Interação: <white>" + interaction);
    }

    private void handleList(@NotNull CommandContext ctx) {
        var sender = ctx.getSender();
        var npcs = npcManager.getAllNPCs();

        if (npcs.isEmpty()) {
            StringUtils.send(sender, "<yellow>Nenhum NPC foi criado ainda.");
            StringUtils.send(sender, "<gray>Use <white>/npc create <id> <gray>para criar um.");
            return;
        }

        StringUtils.send(sender, "<green><bold>NPCs criados <gray>(" + npcs.size() + "):");
        npcs.forEach(handler -> {
            var data = handler.getData();
            var loc = data.location();
            var world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

            StringUtils.send(sender, " <yellow>• " + data.name());
            StringUtils.send(sender, "   <gray>Mundo: <white>" + world + " <gray>X: <white>" + (int) loc.getX()
                                     + " <gray>Y: <white>" + (int) loc.getY() + " <gray>Z: <white>" + (int) loc.getZ());

            if (data.hologramId() != null) {
                StringUtils.send(sender, "   <gray>Holograma: <white>" + data.hologramId());
            }

            if (data.interaction() != null) {
                StringUtils.send(sender, "   <gray>Interação: <white>" + data.interaction());
            }
        });
    }

    private void handleReload(@NotNull CommandContext ctx) throws CommandFailedException {
        npcManager.unloadAll();
        npcManager.load();
        StringUtils.send(ctx.getSender(), "<green>✓ Todos os NPCs foram recarregados do disco!");
    }

    private void requirePlayer(@NotNull CommandContext ctx) throws CommandFailedException {
        if (!(ctx.getSender() instanceof Player)) {
            throw new CommandFailedException("<red>Apenas jogadores podem executar este comando.");
        }
    }

    private List<String> getNPCIds() {
        return npcManager.getAllNPCs().stream()
          .map(handler -> handler.getData().name())
          .collect(Collectors.toList());
    }

    private List<String> getHologramIds() {
        // Este método precisa ser implementado no HologramManager
        // Por enquanto retorna lista vazia
        return new ArrayList<>();
    }

    private List<String> getOnlinePlayerNames() {
        return org.bukkit.Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .collect(Collectors.toList());
    }

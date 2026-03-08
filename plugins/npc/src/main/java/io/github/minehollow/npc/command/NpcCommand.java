package io.github.minehollow.npc.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.npc.NpcPlugin;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcClickType;
import io.github.minehollow.npc.api.actions.BroadcastAction;
import io.github.minehollow.npc.api.actions.CommandAction;
import io.github.minehollow.npc.api.actions.MessageAction;
import io.github.minehollow.npc.api.actions.SoundAction;
import io.github.minehollow.npc.impl.NpcImpl;
import io.github.minehollow.npc.impl.NpcRegistryImpl;
import io.github.minehollow.npc.service.SkinService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class NpcCommand extends SimpleCommand {

    private static final List<String> SUB_COMMANDS = List.of(
        "create", "remove", "tp", "skin", "scale",
        "hologram", "action", "list", "info", "clicktype", "save", "lookatplayer", "reload"
    );

    private static final List<String> HOLOGRAM_SUBS = List.of("set", "add", "remove", "offset");
    private static final List<String> ACTION_SUBS = List.of("add", "clear");
    private static final List<String> ACTION_TYPES = List.of("COMMAND", "MESSAGE", "SOUND", "BROADCAST");

    private static final Pattern VALUE_SIGNATURE_JSON = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\".*\"signature\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_SIGNATURE_SPLIT = Pattern.compile("^([^|;]+)[|;]([^|;]+)$");

    private final NpcPlugin plugin;

    public NpcCommand(@NotNull NpcPlugin plugin) {
        super("npc", "npc.admin");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private NpcRegistryImpl registry() {
        return (NpcRegistryImpl) plugin.getRegistry();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) {
            sendHelp(ctx);
            return;
        }

        switch (sub.toLowerCase()) {
            case "create" -> handleCreate(ctx);
            case "remove" -> handleRemove(ctx);
            case "tp" -> handleTp(ctx);
            case "skin" -> handleSkin(ctx);
            case "scale" -> handleScale(ctx);
            case "hologram" -> handleHologram(ctx);
            case "action" -> handleAction(ctx);
            case "list" -> handleList(ctx);
            case "info" -> handleInfo(ctx);
            case "clicktype" -> handleClickType(ctx);
            case "save" -> handleSave(ctx);
            case "lookatplayer" -> handleLookAtPlayer(ctx);
            case "reload" -> handleReload(ctx);
            default -> sendHelp(ctx);
        }
    }

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) {
            throw fail("§cUso: /npc create <id>");
        }
        String id = ctx.getRawArgOrThrow(1, "§cID do NPC é obrigatório.");
        Player player = ctx.getSenderAsPlayer();

        if (registry().get(id) != null) {
            throw fail("§cNPC '" + id + "' já existe.");
        }

        registry().create(id, player.getLocation());
        ctx.sendMessage("§aNPC §f" + id + " §acriado com sucesso.");
    }

    private void handleRemove(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) {
            throw fail("§cUso: /npc remove <id>");
        }
        String id = ctx.getRawArgOrThrow(1, "§cUso: /npc remove <id>");
        if (!registry().remove(id)) {
            throw fail("§cNPC '" + id + "' não encontrado.");
        }
        ctx.sendMessage("§aNPC §f" + id + " §aremovido.");
    }

    private void handleTp(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) {
            throw fail("§cUso: /npc tp <id>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc tp <id>"));
        Player player = ctx.getSenderAsPlayer();
        npc.teleport(player.getLocation());
        registry().markDirty();
        ctx.sendMessage("§aNPC §f" + npc.getId() + " §ateleportado para sua posição.");
    }

    private void handleSkin(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc skin <id> <player|url|value signature>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc skin <id> <player|url|value signature>"));

        if (ctx.getArgs().length >= 4) {
            String value = ctx.getRawArgOrThrow(2, "§cUso: /npc skin <id> <value> <signature>");
            String signature = ctx.getRawArgOrThrow(3, "§cUso: /npc skin <id> <value> <signature>");
            npc.setSkin(value, signature);
            registry().markDirty();
            ctx.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aatualizada.");
            return;
        }

        String input = ctx.getRawArgOrThrow(2, "§cUso: /npc skin <id> <player|url>");

        SkinService.SkinData inline = tryParseInlineSkinData(input);
        if (inline != null) {
            npc.setSkin(inline.value(), inline.signature());
            registry().markDirty();
            ctx.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aatualizada (value/signature detectado). ");
            return;
        }

        if (input.regionMatches(true, 0, "http", 0, 4)) {
            ctx.sendMessage("§7Buscando skin via MineSkin URL...");
            plugin.getSkinService().fetchByUrl(input).thenAccept(data -> {
                if (data == null) {
                    Tasks.runSync(() -> ctx.sendMessage("§cNão foi possível obter skin da URL informada."));
                    return;
                }
                Tasks.runSync(() -> {
                    npc.setSkin(data.value(), data.signature());
                    registry().markDirty();
                    ctx.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aatualizada (MineSkin URL).");
                });
            });
            return;
        }

        Player online = org.bukkit.Bukkit.getPlayerExact(input);

        if (online != null) {
            var profile = online.getPlayerProfile();
            var prop = profile.getProperties()
                .stream()
                .filter(p -> p.getName().equals("textures"))
                .findFirst().orElse(null);
            if (prop != null) {
                npc.setSkin(prop.getValue(), prop.getSignature() != null ? prop.getSignature() : "");
                registry().markDirty();
                ctx.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aatualizada (jogador online).");
                return;
            }
        }

        ctx.sendMessage("§7Buscando skin de §f" + input + "§7...");
        plugin.getSkinService().fetchByName(input).thenAccept(data -> {
            if (data == null) {
                Tasks.runSync(() -> ctx.sendMessage("§cNão foi possível encontrar a skin de '" + input + "'."));
                return;
            }
            Tasks.runSync(() -> {
                npc.setSkin(data.value(), data.signature());
                registry().markDirty();
                ctx.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aatualizada (Mojang API).");
            });
        });
    }

    private void handleScale(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc scale <id> <valor>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc scale <id> <valor>"));
        double scale;
        try {
            scale = Double.parseDouble(ctx.getRawArgOrThrow(2, "§cUso: /npc scale <id> <valor>"));
        } catch (NumberFormatException e) {
            throw fail("§cValor inválido: " + ctx.getRawArgOrThrow(2, "§cUso: /npc scale <id> <valor>"));
        }
        double min = plugin.getConfig().getDouble("scale.min", 0.1);
        double max = plugin.getConfig().getDouble("scale.max", 5.0);
        if (scale < min || scale > max) {
            throw fail("§cEscala deve estar entre " + min + " e " + max + ".");
        }
        npc.setScale(scale);
        registry().markDirty();
        ctx.sendMessage("§aEscala do NPC §f" + npc.getId() + " §adefinida para §f" + scale + "§a.");
    }

    private void handleHologram(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc hologram <id> <set|add|remove|offset> [args]");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc hologram <id> ..."));
        String action = ctx.getRawArgOrThrow(2, "§cUso: /npc hologram <id> <set|add|remove|offset>").toLowerCase();
        switch (action) {
            case "add" -> {
                if (ctx.isArgsLengthLessThan(4)) {
                    throw fail("§cUso: /npc hologram <id> add <texto>");
                }
                String text = joinArgs(ctx.getArgs(), 3);
                npc.addHologramLine(text);
                ctx.sendMessage("§aLinha adicionada ao holograma.");
            }
            case "set" -> {
                if (ctx.isArgsLengthLessThan(5)) {
                    throw fail("§cUso: /npc hologram <id> set <índice> <texto>");
                }
                int index = Integer.parseInt(ctx.getRawArgOrThrow(3, "§cUso: /npc hologram <id> set <índice> <texto>"));
                String text = joinArgs(ctx.getArgs(), 4);
                npc.setHologramLine(index, text);
                ctx.sendMessage("§aLinha " + index + " atualizada.");
            }
            case "remove" -> {
                if (ctx.isArgsLengthLessThan(4)) {
                    throw fail("§cUso: /npc hologram <id> remove <índice>");
                }
                int index = Integer.parseInt(ctx.getRawArgOrThrow(3, "§cUso: /npc hologram <id> remove <índice>"));
                npc.removeHologramLine(index);
                ctx.sendMessage("§aLinha " + index + " removida.");
            }
            case "offset" -> {
                if (ctx.isArgsLengthLessThan(4)) {
                    throw fail("§cUso: /npc hologram <id> offset <valor>");
                }
                double offset;
                try {
                    offset = Double.parseDouble(ctx.getRawArgOrThrow(3, "§cUso: /npc hologram <id> offset <valor>"));
                } catch (NumberFormatException e) {
                    throw fail("§cValor inválido.");
                }
                npc.setHologramOffset(offset);
                ctx.sendMessage("§aOffset do holograma definido para §f" + offset + "§a.");
            }
            default -> throw fail("§cSubcomando inválido: " + action);
        }
        registry().markDirty();
    }

    private void handleAction(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc action <id> <add|clear> [args]");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc action <id> <add|clear> [args]"));
        String action = ctx.getRawArgOrThrow(2, "§cUso: /npc action <id> <add|clear> [args]").toLowerCase();
        switch (action) {
            case "clear" -> {
                npc.clearActions();
                ctx.sendMessage("§aAções do NPC §f" + npc.getId() + " §alimpas.");
            }
            case "add" -> {
                if (ctx.isArgsLengthLessThan(4)) {
                    throw fail("§cUso: /npc action <id> add <COMMAND|MESSAGE|SOUND|BROADCAST> [args]");
                }
                String type = ctx.getRawArgOrThrow(3, "§cUso: /npc action <id> add <COMMAND|MESSAGE|SOUND|BROADCAST> [args]").toUpperCase();
                switch (type) {
                    case "COMMAND" -> {
                        if (ctx.isArgsLengthLessThan(6)) {
                            throw fail("§cUso: /npc action <id> add COMMAND <CONSOLE|PLAYER> <comando>");
                        }
                        CommandAction.Executor executor;
                        try {
                            executor = CommandAction.Executor.valueOf(
                                ctx.getRawArgOrThrow(4, "§cUso: /npc action <id> add COMMAND <CONSOLE|PLAYER> <comando>").toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw fail("§cExecutor inválido. Use CONSOLE ou PLAYER.");
                        }
                        String cmd = joinArgs(ctx.getArgs(), 5);
                        npc.addAction(new CommandAction(executor, cmd));
                        ctx.sendMessage("§aAção COMMAND adicionada.");
                    }
                    case "MESSAGE" -> {
                        if (ctx.isArgsLengthLessThan(5)) {
                            throw fail("§cUso: /npc action <id> add MESSAGE <mensagem>");
                        }
                        npc.addAction(new MessageAction(joinArgs(ctx.getArgs(), 4)));
                        ctx.sendMessage("§aAção MESSAGE adicionada.");
                    }
                    case "SOUND" -> {
                        if (ctx.isArgsLengthLessThan(5)) {
                            throw fail("§cUso: /npc action <id> add SOUND <som> [volume] [pitch]");
                        }
                        String soundStr = ctx.getRawArgOrThrow(4, "§cUso: /npc action <id> add SOUND <som> [volume] [pitch]");
                        String soundKey = soundStr.toLowerCase().replace('_', '.');
                        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));
                        if (sound == null) {
                            sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundStr.toLowerCase()));
                        }
                        if (sound == null) {
                            throw fail("§cSom inválido: " + soundStr);
                        }
                        float volume = ctx.getArgs().length > 5 ? Float.parseFloat(
                            ctx.getRawArgOrThrow(5, "§cUso: /npc action <id> add SOUND <som> <volume>")) : 1.0f;
                        float pitch = ctx.getArgs().length > 6 ? Float.parseFloat(
                            ctx.getRawArgOrThrow(6, "§cUso: /npc action <id> add SOUND <som> <pitch>")) : 1.0f;
                        npc.addAction(new SoundAction(sound, volume, pitch));
                        ctx.sendMessage("§aAção SOUND adicionada.");
                    }
                    case "BROADCAST" -> {
                        if (ctx.isArgsLengthLessThan(5)) {
                            throw fail("§cUso: /npc action <id> add BROADCAST <mensagem>");
                        }
                        npc.addAction(new BroadcastAction(joinArgs(ctx.getArgs(), 4)));
                        ctx.sendMessage("§aAção BROADCAST adicionada.");
                    }
                    default -> throw fail("§cTipo de ação inválido: " + type);
                }
            }
            default -> throw fail("§cSubcomando inválido: " + action);
        }
        registry().markDirty();
    }

    private void handleList(@NotNull CommandContext ctx) {
        var npcs = registry().getAll();
        if (npcs.isEmpty()) {
            ctx.sendMessage("§7Nenhum NPC registrado.");
            return;
        }
        ctx.sendMessage("§6§lNPCs registrados (" + npcs.size() + "):");
        for (Npc npc : npcs) {
            Location loc = npc.getLocation();
            ctx.sendMessage("§7- §f" + npc.getId() + " §7em §f"
                            + (loc.getWorld() != null ? loc.getWorld().getName() : "?")
                            + " §7(" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) + ")");
        }
    }

    private void handleInfo(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) {
            throw fail("§cUso: /npc info <id>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc info <id>"));
        Location loc = npc.getLocation();
        npc.getSkinValue();
        ctx.sendMessage(
            "§6§l" + npc.getId(),
            "§7Mundo: §f" + (loc.getWorld() != null ? loc.getWorld().getName() : "?"),
            "§7Posição: §f" + String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()),
            "§7Escala: §f" + npc.getScale(),
            "§7Skin: §f" + "definida",
            "§7Holograma: §f" + npc.getHologramLines().size() + " linhas (offset: " + npc.getHologramOffset() + ")",
            "§7Ações: §f" + npc.getActions().size(),
            "§7Tipo de clique: §f" + npc.getClickType().name()
        );
    }

    private void handleClickType(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc clicktype <id> <RIGHT|LEFT|SHIFT_RIGHT|SHIFT_LEFT|ANY>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc clicktype <id> <tipo>"));
        NpcClickType type;
        try {
            type = NpcClickType.valueOf(ctx.getRawArgOrThrow(2, "§cUso: /npc clicktype <id> <tipo>").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw fail("§cTipo inválido: " + ctx.getRawArgOrThrow(2, "§cUso: /npc clicktype <id> <tipo>"));
        }
        npc.setClickType(type);
        registry().markDirty();

        ctx.sendMessage("§aTipo de clique do NPC §f" + npc.getId() + " §adefinido para §f" + type.name() + "§a.");
    }

    private void handleSave(@NotNull CommandContext ctx) {
        registry().saveAll();
        ctx.sendMessage("§aNPCs salvos.");
    }

    private void handleLookAtPlayer(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) {
            throw fail("§cUso: /npc lookatplayer <id> <on|off>");
        }
        Npc npc = requireNpc(ctx.getRawArgOrThrow(1, "§cUso: /npc lookatplayer <id> <on|off>"));
        String arg = ctx.getRawArgOrThrow(2, "§cUso: /npc lookatplayer <id> <on|off>");
        boolean enable;
        if (arg.equalsIgnoreCase("on")) {
            enable = true;
        } else if (arg.equalsIgnoreCase("off")) {
            enable = false;
        } else {
            throw fail("§cUso: /npc lookatplayer <id> <on|off>");
        }

        if (npc instanceof NpcImpl impl) {
            impl.getConfig().setLookAtNearestPlayer(enable);
            registry().markDirty();
            ctx.sendMessage(enable
                ? "§aNPC §f" + npc.getId() + " §aagora olha para jogadores próximos."
                : "§eNPC §f" + npc.getId() + " §enão olha mais para jogadores próximos.");
            return;
        }

        throw fail("§cNão foi possível alterar lookatplayer para este NPC.");
    }

    private void handleReload(@NotNull CommandContext ctx) throws CommandFailedException {
        registry().unloadAll();
        registry().loadAll();
        ctx.sendMessage("§aNPCs recarregados do disco!");
    }

    private @NotNull Npc requireNpc(@NotNull String id) throws CommandFailedException {
        Npc npc = registry().get(id);
        if (npc == null) {
            throw fail("§cNPC '" + id + "' não encontrado.");
        }
        return npc;
    }

    private void sendHelp(@NotNull CommandContext ctx) {
        ctx.sendMessage(
            "§6§l/npc §7- Comandos:",
            "§f/npc create <id> §7— Cria um NPC",
            "§f/npc remove <id> §7— Remove um NPC",
            "§f/npc tp <id> §7— Teleporta o NPC",
            "§f/npc skin <id> <player|url|value sig> §7— Define a skin",
            "§f/npc scale <id> <valor> §7— Define a escala",
            "§f/npc hologram <id> <set|add|remove|offset> §7— Holograma",
            "§f/npc action <id> <add|clear> §7— Ações",
            "§f/npc clicktype <id> <tipo> §7— Tipo de clique",
            "§f/npc list §7— Lista NPCs",
            "§f/npc info <id> §7— Informações",
            "§f/npc save §7— Salva NPCs",
            "§f/npc lookatplayer <id> <on|off> §7— Ativa/desativa olhar para jogador próximo",
            "§f/npc reload §7— Recarrega NPCs do disco"
        );
    }

    private static @NotNull CommandFailedException fail(@NotNull String message) {
        return new CommandFailedException(message);
    }

    private static @NotNull String joinArgs(@NotNull String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private SkinService.SkinData tryParseInlineSkinData(@NotNull String input) {
        Matcher split = VALUE_SIGNATURE_SPLIT.matcher(input.trim());
        if (split.matches()) {
            return new SkinService.SkinData(split.group(1), split.group(2));
        }
        Matcher json = VALUE_SIGNATURE_JSON.matcher(input);
        if (json.find()) {
            return new SkinService.SkinData(json.group(1), json.group(2));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, new ArrayList<>());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("remove", "tp", "skin", "scale", "hologram", "action", "info", "clicktype", "lookatplayer").contains(sub)) {
                List<String> ids = registry().getAll().stream().map(Npc::getId).toList();
                return StringUtil.copyPartialMatches(args[1], ids, new ArrayList<>());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("lookatplayer")) {
                return StringUtil.copyPartialMatches(args[2], List.of("on", "off"), new ArrayList<>());
            }
            return switch (args[0].toLowerCase()) {
                case "hologram" -> StringUtil.copyPartialMatches(args[2], HOLOGRAM_SUBS, new ArrayList<>());
                case "action" -> StringUtil.copyPartialMatches(args[2], ACTION_SUBS, new ArrayList<>());
                case "clicktype" -> StringUtil.copyPartialMatches(
                    args[2],
                    Arrays.stream(NpcClickType.values()).map(Enum::name).toList(), new ArrayList<>()
                );
                default -> Collections.emptyList();
            };
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("action") && args[2].equalsIgnoreCase("add")) {
            return StringUtil.copyPartialMatches(args[3], ACTION_TYPES, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

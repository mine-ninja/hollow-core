package io.github.minehollow.hologram.command;

import io.github.minehollow.hologram.Hologram;
import io.github.minehollow.hologram.HologramPlugin;
import io.github.minehollow.hologram.impl.HologramRegistryImpl;
import io.github.minehollow.hologram.line.*;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin command for managing holograms.
 * <p>
 * Usage:
 * <pre>
 *   /hologram create <id>
 *   /hologram remove <id>
 *   /hologram tp <id>
 *   /hologram addline <id> text <minimessage>
 *   /hologram addline <id> item <material> [scale]
 *   /hologram addline <id> block <material> [scale]
 *   /hologram setline <id> <index> text <minimessage>
 *   /hologram setline <id> <index> item <material> [scale]
 *   /hologram setline <id> <index> block <material> [scale]
 *   /hologram removeline <id> <index>
 *   /hologram spacing <id> <value>
 *   /hologram persistent <id> <true|false>
 *   /hologram list
 *   /hologram info <id>
 *   /hologram save
 *   /hologram reload
 * </pre>
 */
public class HologramCommand extends SimpleCommand {

    private static final List<String> SUB_COMMANDS = List.of(
            "create", "remove", "tp", "addline", "setline", "removeline",
            "spacing", "persistent", "list", "info", "save", "reload"
    );

    private static final List<String> LINE_TYPES = List.of("text", "item", "block");

    private final HologramPlugin plugin;

    public HologramCommand(@NotNull HologramPlugin plugin) {
        super("hologram", "hologram.admin");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private HologramRegistryImpl registry() {
        return (HologramRegistryImpl) plugin.getRegistry();
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
            case "addline" -> handleAddLine(ctx);
            case "setline" -> handleSetLine(ctx);
            case "removeline" -> handleRemoveLine(ctx);
            case "spacing" -> handleSpacing(ctx);
            case "persistent" -> handlePersistent(ctx);
            case "list" -> handleList(ctx);
            case "info" -> handleInfo(ctx);
            case "save" -> handleSave(ctx);
            case "reload" -> handleReload(ctx);
            default -> sendHelp(ctx);
        }
    }

    // ── Handlers ─────────────────────────────────────────────

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) throw fail("§cUso: /hologram create <id>");
        String id = ctx.getRawArgOrThrow(1, "§cID do holograma é obrigatório.");
        Player player = ctx.getSenderAsPlayer();

        if (registry().get(id) != null) {
            throw fail("§cHolograma '" + id + "' já existe.");
        }

        registry().create(id, player.getLocation());
        ctx.sendMessage("§aHolograma §f" + id + " §acriado com sucesso.");
    }

    private void handleRemove(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) throw fail("§cUso: /hologram remove <id>");
        String id = ctx.getRawArgOrThrow(1, "§cUso: /hologram remove <id>");
        if (!registry().remove(id)) {
            throw fail("§cHolograma '" + id + "' não encontrado.");
        }
        ctx.sendMessage("§aHolograma §f" + id + " §aremovido.");
    }

    private void handleTp(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) throw fail("§cUso: /hologram tp <id>");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram tp <id>"));
        Player player = ctx.getSenderAsPlayer();
        holo.teleport(player.getLocation());
        registry().markDirty();
        ctx.sendMessage("§aHolograma §f" + holo.getId() + " §ateleportado para sua posição.");
    }

    private void handleAddLine(@NotNull CommandContext ctx) throws CommandFailedException {
        // /hologram addline <id> <text|item|block> <value> [scale]
        if (ctx.isArgsLengthLessThan(4)) throw fail("§cUso: /hologram addline <id> <text|item|block> <valor> [escala]");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram addline <id> ..."));
        String lineType = ctx.getRawArgOrThrow(2, "§cTipo de linha é obrigatório.").toLowerCase();

        HologramLine line = parseLine(ctx, lineType, 3);
        holo.addLine(line);
        registry().markDirty();
        ctx.sendMessage("§aLinha adicionada ao holograma §f" + holo.getId() + "§a.");
    }

    private void handleSetLine(@NotNull CommandContext ctx) throws CommandFailedException {
        // /hologram setline <id> <index> <text|item|block> <value> [scale]
        if (ctx.isArgsLengthLessThan(5)) throw fail("§cUso: /hologram setline <id> <índice> <text|item|block> <valor> [escala]");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram setline <id> ..."));
        int index;
        try {
            index = Integer.parseInt(ctx.getRawArgOrThrow(2, "§cÍndice é obrigatório."));
        } catch (NumberFormatException e) {
            throw fail("§cÍndice inválido.");
        }

        if (index < 0 || index >= holo.getLines().size()) {
            throw fail("§cÍndice fora do intervalo (0-" + (holo.getLines().size() - 1) + ").");
        }

        String lineType = ctx.getRawArgOrThrow(3, "§cTipo de linha é obrigatório.").toLowerCase();
        HologramLine line = parseLine(ctx, lineType, 4);
        holo.setLine(index, line);
        registry().markDirty();
        ctx.sendMessage("§aLinha " + index + " atualizada no holograma §f" + holo.getId() + "§a.");
    }

    private void handleRemoveLine(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) throw fail("§cUso: /hologram removeline <id> <índice>");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram removeline <id> <índice>"));
        int index;
        try {
            index = Integer.parseInt(ctx.getRawArgOrThrow(2, "§cÍndice é obrigatório."));
        } catch (NumberFormatException e) {
            throw fail("§cÍndice inválido.");
        }

        if (index < 0 || index >= holo.getLines().size()) {
            throw fail("§cÍndice fora do intervalo (0-" + (holo.getLines().size() - 1) + ").");
        }

        holo.removeLine(index);
        registry().markDirty();
        ctx.sendMessage("§aLinha " + index + " removida do holograma §f" + holo.getId() + "§a.");
    }

    private void handleSpacing(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) throw fail("§cUso: /hologram spacing <id> <valor>");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram spacing <id> <valor>"));
        double spacing;
        try {
            spacing = Double.parseDouble(ctx.getRawArgOrThrow(2, "§cUso: /hologram spacing <id> <valor>"));
        } catch (NumberFormatException e) {
            throw fail("§cValor inválido.");
        }
        if (spacing < 0 || spacing > 10) throw fail("§cEspaçamento deve estar entre 0 e 10.");

        holo.setLineSpacing(spacing);
        registry().markDirty();
        ctx.sendMessage("§aEspaçamento do holograma §f" + holo.getId() + " §adefinido para §f" + spacing + "§a.");
    }

    private void handlePersistent(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(3)) throw fail("§cUso: /hologram persistent <id> <true|false>");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram persistent <id> <true|false>"));
        String val = ctx.getRawArgOrThrow(2, "§cUso: /hologram persistent <id> <true|false>").toLowerCase();
        boolean persistent;
        if (val.equals("true")) {
            persistent = true;
        } else if (val.equals("false")) {
            persistent = false;
        } else {
            throw fail("§cUse 'true' ou 'false'.");
        }
        holo.setPersistent(persistent);
        registry().markDirty();
        ctx.sendMessage("§aHolograma §f" + holo.getId() + " §a" + (persistent ? "agora é persistente." : "não é mais persistente."));
    }

    private void handleList(@NotNull CommandContext ctx) {
        var holograms = registry().getAll();
        if (holograms.isEmpty()) {
            ctx.sendMessage("§7Nenhum holograma registrado.");
            return;
        }
        ctx.sendMessage("§6§lHologramas registrados (" + holograms.size() + "):");
        for (Hologram holo : holograms) {
            Location loc = holo.getLocation();
            String persistent = holo.isPersistent() ? "§a✓" : "§c✗";
            ctx.sendMessage("§7- §f" + holo.getId() + " " + persistent + " §7em §f"
                    + (loc.getWorld() != null ? loc.getWorld().getName() : "?")
                    + " §7(" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) + ")"
                    + " §7[" + holo.getLines().size() + " linhas]");
        }
    }

    private void handleInfo(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLengthLessThan(2)) throw fail("§cUso: /hologram info <id>");
        Hologram holo = requireHologram(ctx.getRawArgOrThrow(1, "§cUso: /hologram info <id>"));
        Location loc = holo.getLocation();

        ctx.sendMessage(
                "§6§l" + holo.getId(),
                "§7Mundo: §f" + (loc.getWorld() != null ? loc.getWorld().getName() : "?"),
                "§7Posição: §f" + String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()),
                "§7Espaçamento: §f" + holo.getLineSpacing(),
                "§7Persistente: §f" + (holo.isPersistent() ? "Sim" : "Não"),
                "§7Linhas: §f" + holo.getLines().size()
        );

        for (int i = 0; i < holo.getLines().size(); i++) {
            HologramLine line = holo.getLines().get(i);
            String lineInfo = switch (line.getType()) {
                case TEXT -> {
                    TextDisplayHologramLine textLine = (TextDisplayHologramLine) line;
                    yield "§7  [" + i + "] §eTEXT §7— §f" + textLine.getText();
                }
                case ITEM -> {
                    ItemDisplayHologramLine itemLine = (ItemDisplayHologramLine) line;
                    yield "§7  [" + i + "] §bITEM §7— §f" + itemLine.getMaterial().name() + " §7(escala: " + itemLine.getScale() + ")";
                }
                case BLOCK -> {
                    BlockDisplayHologramLine blockLine = (BlockDisplayHologramLine) line;
                    yield "§7  [" + i + "] §dBLOCK §7— §f" + blockLine.getMaterial().name() + " §7(escala: " + blockLine.getScale() + ")";
                }
            };
            ctx.sendMessage(lineInfo);
        }
    }

    private void handleSave(@NotNull CommandContext ctx) {
        registry().saveAll();
        ctx.sendMessage("§aHologramas salvos.");
    }

    private void handleReload(@NotNull CommandContext ctx) {
        registry().unloadAll();
        registry().loadAll();
        ctx.sendMessage("§aHologramas recarregados do disco!");
    }

    // ── Utilities ────────────────────────────────────────────

    private @NotNull HologramLine parseLine(@NotNull CommandContext ctx, @NotNull String type, int valueArgStart) throws CommandFailedException {
        return switch (type) {
            case "text" -> {
                String text = joinArgs(ctx.getArgs(), valueArgStart);
                if (text.isEmpty()) throw fail("§cTexto não pode ser vazio.");
                yield new TextDisplayHologramLine(text);
            }
            case "item" -> {
                String materialName = ctx.getRawArgOrThrow(valueArgStart, "§cMaterial é obrigatório.");
                Material material = parseMaterial(materialName);
                float scale = parseOptionalFloat(ctx, valueArgStart + 1, 1.0f);
                yield new ItemDisplayHologramLine(material, scale);
            }
            case "block" -> {
                String materialName = ctx.getRawArgOrThrow(valueArgStart, "§cMaterial é obrigatório.");
                Material material = parseMaterial(materialName);
                if (!material.isBlock()) throw fail("§cMaterial '" + materialName + "' não é um bloco.");
                float scale = parseOptionalFloat(ctx, valueArgStart + 1, 0.5f);
                yield new BlockDisplayHologramLine(material, scale);
            }
            default -> throw fail("§cTipo de linha inválido: " + type + ". Use 'text', 'item' ou 'block'.");
        };
    }

    private @NotNull Material parseMaterial(@NotNull String name) throws CommandFailedException {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw fail("§cMaterial inválido: " + name);
        }
    }

    private float parseOptionalFloat(@NotNull CommandContext ctx, int index, float defaultValue) {
        String raw = ctx.getRawArgOrNull(index);
        if (raw == null) return defaultValue;
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private @NotNull Hologram requireHologram(@NotNull String id) throws CommandFailedException {
        Hologram holo = registry().get(id);
        if (holo == null) throw fail("§cHolograma '" + id + "' não encontrado.");
        return holo;
    }

    private void sendHelp(@NotNull CommandContext ctx) {
        ctx.sendMessage(
                "§6§l/hologram §7— Comandos:",
                "§f/hologram create <id> §7— Cria um holograma",
                "§f/hologram remove <id> §7— Remove um holograma",
                "§f/hologram tp <id> §7— Teleporta o holograma",
                "§f/hologram addline <id> <text|item|block> <valor> [escala] §7— Adiciona uma linha",
                "§f/hologram setline <id> <índice> <text|item|block> <valor> [escala] §7— Edita uma linha",
                "§f/hologram removeline <id> <índice> §7— Remove uma linha",
                "§f/hologram spacing <id> <valor> §7— Define espaçamento entre linhas",
                "§f/hologram persistent <id> <true|false> §7— Alterna persistência",
                "§f/hologram list §7— Lista hologramas",
                "§f/hologram info <id> §7— Informações",
                "§f/hologram save §7— Salva hologramas",
                "§f/hologram reload §7— Recarrega hologramas do disco"
        );
    }

    private static @NotNull CommandFailedException fail(@NotNull String message) {
        return new CommandFailedException(message);
    }

    private static @NotNull String joinArgs(@NotNull String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ── Tab-complete ─────────────────────────────────────────

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, new ArrayList<>());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("remove", "tp", "addline", "setline", "removeline", "spacing", "persistent", "info").contains(sub)) {
                List<String> ids = registry().getAll().stream().map(Hologram::getId).toList();
                return StringUtil.copyPartialMatches(args[1], ids, new ArrayList<>());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("addline")) {
                return StringUtil.copyPartialMatches(args[2], LINE_TYPES, new ArrayList<>());
            }
            if (sub.equals("setline")) {
                Hologram holo = registry().get(args[1]);
                if (holo != null) {
                    List<String> indices = new ArrayList<>();
                    for (int i = 0; i < holo.getLines().size(); i++) indices.add(String.valueOf(i));
                    return StringUtil.copyPartialMatches(args[2], indices, new ArrayList<>());
                }
            }
            if (sub.equals("removeline")) {
                Hologram holo = registry().get(args[1]);
                if (holo != null) {
                    List<String> indices = new ArrayList<>();
                    for (int i = 0; i < holo.getLines().size(); i++) indices.add(String.valueOf(i));
                    return StringUtil.copyPartialMatches(args[2], indices, new ArrayList<>());
                }
            }
            if (sub.equals("persistent")) {
                return StringUtil.copyPartialMatches(args[2], List.of("true", "false"), new ArrayList<>());
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setline")) {
                return StringUtil.copyPartialMatches(args[3], LINE_TYPES, new ArrayList<>());
            }
            if (sub.equals("addline")) {
                String type = args[2].toLowerCase();
                if (type.equals("item") || type.equals("block")) {
                    return matchMaterials(args[3], type.equals("block"));
                }
            }
        }

        if (args.length == 5) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setline")) {
                String type = args[3].toLowerCase();
                if (type.equals("item") || type.equals("block")) {
                    return matchMaterials(args[4], type.equals("block"));
                }
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<String> matchMaterials(@NotNull String prefix, boolean blocksOnly) {
        List<String> materials = Arrays.stream(Material.values())
                .filter(m -> !blocksOnly || m.isBlock())
                .filter(m -> !m.isLegacy())
                .map(m -> m.name().toLowerCase())
                .collect(Collectors.toList());
        return StringUtil.copyPartialMatches(prefix.toLowerCase(), materials, new ArrayList<>());
    }
}


package io.github.minehollow.zones.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.zones.ZonesPlugin;
import io.github.minehollow.zones.model.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ZonesCommand extends SimpleCommand {

    private final ZonesPlugin plugin;

    public ZonesCommand(@NotNull ZonesPlugin plugin) {
        super("zones", "zones.admin");
        setAliases(List.of("zone", "zona", "region"));
        this.plugin                = plugin;
        this.playersOnly = false;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) {
            sendUsage(ctx);
            return;
        }

        switch (sub.toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(ctx);
            case "delete" -> handleDelete(ctx);
            case "flag" -> handleFlag(ctx);
            case "addmember" -> handleAddMember(ctx);
            case "info" -> handleInfo(ctx);
            case "reload" -> handleReload(ctx);
            default -> sendUsage(ctx);
        }
    }

    // /zones create <id> <chunk|cuboid> [priority]
    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        String id = ctx.getRawArgOrThrow(1, "§cUso: /zones create <id> <chunk|cuboid> [priority]");
        String typeStr = ctx.getRawArgOrThrow(2, "§cUso: /zones create <id> <chunk|cuboid> [priority]");

        if (plugin.getZoneManager().getZone(id) != null) {
            throw new CommandFailedException("§cZona '" + id + "' já existe.");
        }

        ZoneType type;
        try {
            type = ZoneType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException("§cTipo inválido. Use: chunk ou cuboid");
        }

        int priority = 0;
        String prioStr = ctx.getRawArgOrNull(3);
        if (prioStr != null) {
            try { priority = Integer.parseInt(prioStr); }
            catch (NumberFormatException e) { throw new CommandFailedException("§cPrioridade inválida."); }
        }

        // Get WorldEdit selection (same API as mines module)
        Region region;
        try {
            var actor = BukkitAdapter.adapt(player);
            var session = WorldEdit.getInstance().getSessionManager().get(actor);
            region = session.getSelection(actor.getWorld());
        } catch (IncompleteRegionException e) {
            throw new CommandFailedException("§cVocê precisa fazer uma seleção com o WorldEdit primeiro (//pos1 e //pos2).");
        }

        if (region == null) {
            throw new CommandFailedException("§cVocê precisa fazer uma seleção com o WorldEdit primeiro (//pos1 e //pos2).");
        }

        var loc1 = BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint());
        var loc2 = BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint());
        String world = player.getWorld().getName();

        ZoneBounds bounds = new ZoneBounds(
            loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ(),
            loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()
        );

        Zone zone = new Zone(id, type, world, id, priority, bounds,
            new EnumMap<>(ZoneFlag.class), new ObjectOpenHashSet<>());

        plugin.getZoneManager().addZone(zone);
        plugin.getZoneManager().saveAll();
        ctx.sendMessage("§aZona '" + id + "' criada com sucesso! (tipo=" + type + ", prioridade=" + priority + ")");
        ctx.sendMessage("§7Bounds: [" + loc1.getBlockX() + ", " + loc1.getBlockY() + ", " + loc1.getBlockZ()
            + "] → [" + loc2.getBlockX() + ", " + loc2.getBlockY() + ", " + loc2.getBlockZ() + "]");
    }

    // /zones delete <id>
    private void handleDelete(@NotNull CommandContext ctx) throws CommandFailedException {
        String id = ctx.getRawArgOrThrow(1, "§cUso: /zones delete <id>");
        if (plugin.getZoneManager().getZone(id) == null) {
            throw new CommandFailedException("§cZona '" + id + "' não encontrada.");
        }
        plugin.getZoneManager().removeZone(id);
        plugin.getZoneManager().saveAll();
        ctx.sendMessage("§aZona '" + id + "' removida com sucesso.");
    }

    // /zones flag <id> <flag> <allow|deny|none>
    private void handleFlag(@NotNull CommandContext ctx) throws CommandFailedException {
        String id = ctx.getRawArgOrThrow(1, "§cUso: /zones flag <id> <flag> <allow|deny|none>");
        String flagStr = ctx.getRawArgOrThrow(2, "§cUso: /zones flag <id> <flag> <allow|deny|none>");
        String stateStr = ctx.getRawArgOrThrow(3, "§cUso: /zones flag <id> <flag> <allow|deny|none>");

        Zone zone = plugin.getZoneManager().getZone(id);
        if (zone == null) throw new CommandFailedException("§cZona '" + id + "' não encontrada.");

        ZoneFlag flag = ZoneFlag.fromKey(flagStr);
        if (flag == null) throw new CommandFailedException("§cFlag inválida: " + flagStr);

        ZoneFlagState state = ZoneFlagState.fromString(stateStr);
        if (state == null) throw new CommandFailedException("§cEstado inválido. Use: allow, deny ou none");

        zone.setFlag(flag, state);
        plugin.getZoneManager().saveAll();
        ctx.sendMessage("§aFlag '" + flag.configKey() + "' da zona '" + id + "' definida para " + state.name().toLowerCase(Locale.ROOT) + ".");
    }

    // /zones addmember <id> <player>
    private void handleAddMember(@NotNull CommandContext ctx) throws CommandFailedException {
        String id = ctx.getRawArgOrThrow(1, "§cUso: /zones addmember <id> <player>");
        String playerName = ctx.getRawArgOrThrow(2, "§cUso: /zones addmember <id> <player>");

        Zone zone = plugin.getZoneManager().getZone(id);
        if (zone == null) throw new CommandFailedException("§cZona '" + id + "' não encontrada.");

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) throw new CommandFailedException("§cJogador '" + playerName + "' não encontrado.");

        zone.addMember(target.getUniqueId());
        plugin.getZoneManager().saveAll();
        ctx.sendMessage("§a" + target.getName() + " adicionado como membro da zona '" + id + "'.");
    }

    // /zones info
    private void handleInfo(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        Location loc = player.getLocation();

        final int[] count = {0};
        ctx.sendMessage("§6§l═══════════════════════════════════════════");
        ctx.sendMessage("§e§l Zonas na sua posição:");
        ctx.sendMessage("§6§l═══════════════════════════════════════════");

        plugin.getZoneManager().forEachZoneAt(loc, z -> {
            count[0]++;
            ctx.sendMessage("§b§l#" + count[0] + " §f§l" + z.getDisplayName());
            ctx.sendMessage("  §7ID: §a" + z.getId() + " §8| §7Tipo: §a" + z.getType() + " §8| §7Prioridade: §a" + z.getPriority());
            ctx.sendMessage("  §7Mundo: §a" + z.getWorld());
            ctx.sendMessage("  §7Bounds: §a[" + z.getBounds().minX() + ", " + z.getBounds().minY() + ", " + z.getBounds().minZ() + "] §8→ §a[" + z.getBounds().maxX() + ", " + z.getBounds().maxY() + ", " + z.getBounds().maxZ() + "]");
            StringBuilder flagsLine = new StringBuilder("  §7Flags: ");
            if (z.getFlags().isEmpty()) {
                flagsLine.append("§8nenhuma");
            } else {
                z.getFlags().forEach((flag, state) ->
                    flagsLine.append(state == ZoneFlagState.DENY ? "§c" : "§a")
                        .append(flag.configKey()).append("=").append(state.name().toLowerCase(Locale.ROOT))
                        .append("§7, "));
                if (flagsLine.lastIndexOf(", ") == flagsLine.length() - 3) {
                    flagsLine.setLength(flagsLine.length() - 3);
                }
            }
            ctx.sendMessage(flagsLine.toString());
            ctx.sendMessage("§7-------------------------------------------");
        });

        if (count[0] == 0) {
            ctx.sendMessage("§7Você não está em nenhuma zona.");
        }
        ctx.sendMessage("§6§l═══════════════════════════════════════════");
    }

    // /zones reload
    private void handleReload(@NotNull CommandContext ctx) {
        plugin.getZoneManager().reload();
        ctx.sendMessage("§aZones recarregadas com sucesso! (" + plugin.getZoneManager().getZones().size() + " zonas)");
    }

    private void sendUsage(@NotNull CommandContext ctx) {
        ctx.sendMessage("§6═══ Zones ═══");
        ctx.sendMessage("§e/zones create <id> <chunk|cuboid> [priority]");
        ctx.sendMessage("§e/zones delete <id>");
        ctx.sendMessage("§e/zones flag <id> <flag> <allow|deny|none>");
        ctx.sendMessage("§e/zones addmember <id> <player>");
        ctx.sendMessage("§e/zones info");
        ctx.sendMessage("§e/zones reload");
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) {
            return List.of("create", "delete", "flag", "addmember", "info", "reload");
        }

        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) return Collections.emptyList();

        return switch (sub.toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (ctx.isArgsLength(3)) yield List.of("chunk", "cuboid");
                yield Collections.emptyList();
            }
            case "delete", "addmember" -> {
                if (ctx.isArgsLength(2)) yield new ArrayList<>(plugin.getZoneManager().getZones().keySet());
                if ("addmember".equals(sub.toLowerCase(Locale.ROOT)) && ctx.isArgsLength(3)) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                }
                yield Collections.emptyList();
            }
            case "flag" -> {
                if (ctx.isArgsLength(2)) yield new ArrayList<>(plugin.getZoneManager().getZones().keySet());
                if (ctx.isArgsLength(3)) yield Arrays.stream(ZoneFlag.values()).map(ZoneFlag::configKey).toList();
                if (ctx.isArgsLength(4)) yield List.of("allow", "deny", "none");
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}


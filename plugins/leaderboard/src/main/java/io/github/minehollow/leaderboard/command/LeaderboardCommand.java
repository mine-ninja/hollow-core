package io.github.minehollow.leaderboard.command;

import io.github.minehollow.leaderboard.LeaderboardManager;
import io.github.minehollow.leaderboard.LeaderboardPlugin;
import io.github.minehollow.leaderboard.menu.LeaderboardListMenu;
import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.stats.StatPeriod;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /leaderboard (aliases: /lb, /ranking, /top)
 *
 * Subcommands:
 *   /lb <id>                          — view a leaderboard in chat
 *   /lb create <id> <statKey> [period] — create a leaderboard
 *   /lb remove <id>                   — remove a leaderboard
 *   /lb hologram <id>                 — set hologram at current location
 *   /lb hologram <id> remove          — remove hologram
 *   /lb list                          — list all leaderboards
 *   /lb info <id>                     — show leaderboard details
 *   /lb reload                        — reload from config
 *   /lb refresh                       — force data refresh
 */
public class LeaderboardCommand extends SimpleCommand {

    private final LeaderboardPlugin plugin;

    public LeaderboardCommand(@NotNull LeaderboardPlugin plugin) {
        super("leaderboard", "leaderboard.admin");
        this.setAliases(List.of("lb", "ranking", "top"));
        this.plugin = plugin;
    }

    private LeaderboardManager manager() {
        return plugin.getManager();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length == 0) {
            // No args — open menu for players, show help for console
            Player player = ctx.getSenderAsPlayer();
            if (player != null) {
                MenuUtil.openMenu(player, LeaderboardListMenu.class);
            } else {
                sendHelp(ctx);
            }
            return;
        }

        String sub = ctx.getArgs()[0].toLowerCase();
        switch (sub) {
            case "menu" -> {
                Player player = ctx.getSenderAsPlayer();
                if (player == null) throw fail("§cApenas jogadores podem abrir o menu.");
                MenuUtil.openMenu(player, LeaderboardListMenu.class);
            }
            case "create", "criar" -> handleCreate(ctx);
            case "remove", "remover", "delete" -> handleRemove(ctx);
            case "hologram", "holo" -> handleHologram(ctx);
            case "list", "lista" -> handleList(ctx);
            case "info" -> handleInfo(ctx);
            case "reload" -> handleReload(ctx);
            case "refresh", "atualizar" -> handleRefresh(ctx);
            case "help", "ajuda" -> sendHelp(ctx);
            default -> handleView(ctx, sub);
        }
    }

    // ── View ─────────────────────────────────────────────────

    private void handleView(@NotNull CommandContext ctx, @NotNull String id) throws CommandFailedException {
        LeaderboardConfig config = manager().get(id);
        if (config == null) throw fail("§cLeaderboard '" + id + "' não encontrada.");

        Player player = ctx.getSenderAsPlayer();
        // Build chat lines async (getRank is blocking)
        Tasks.runAsync(() -> {
            List<Component> lines = manager().buildChatLines(id, player);
            Tasks.runSync(() -> {
                for (Component line : lines) {
                    ctx.getSender().sendMessage(line);
                }
            });
        });
    }

    // ── Create ───────────────────────────────────────────────

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePermission(ctx, "leaderboard.admin");
        // /lb create <id> <statKey> [period]
        if (ctx.getArgs().length < 3) throw fail("§cUso: /lb create <id> <statKey> [period]");

        String id = ctx.getArgs()[1].toLowerCase();
        String statKey = ctx.getArgs()[2];
        StatPeriod period = StatPeriod.ALLTIME;
        if (ctx.getArgs().length >= 4) {
            try {
                period = StatPeriod.valueOf(ctx.getArgs()[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw fail("§cPeríodo inválido. Use: DAILY, WEEKLY, MONTHLY, ALLTIME");
            }
        }

        Player player = ctx.getSenderAsPlayer();
        manager().create(id, statKey, period, player != null ? player.getLocation() : null);
        ctx.sendMessage("§aLeaderboard §f" + id + " §acriada com stat §f" + statKey + " §a(" + period.name() + ").");

        // Trigger initial data load
        Tasks.runAsync(() -> manager().refreshAll());
    }

    // ── Remove ───────────────────────────────────────────────

    private void handleRemove(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePermission(ctx, "leaderboard.admin");
        if (ctx.getArgs().length < 2) throw fail("§cUso: /lb remove <id>");

        String id = ctx.getArgs()[1].toLowerCase();
        if (manager().get(id) == null) throw fail("§cLeaderboard '" + id + "' não encontrada.");

        manager().remove(id);
        ctx.sendMessage("§aLeaderboard §f" + id + " §aremovida.");
    }

    // ── Hologram ─────────────────────────────────────────────

    private void handleHologram(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePermission(ctx, "leaderboard.admin");
        if (ctx.getArgs().length < 2) throw fail("§cUso: /lb hologram <id> [remove]");

        String id = ctx.getArgs()[1].toLowerCase();
        LeaderboardConfig config = manager().get(id);
        if (config == null) throw fail("§cLeaderboard '" + id + "' não encontrada.");

        if (ctx.getArgs().length >= 3 && ctx.getArgs()[2].equalsIgnoreCase("remove")) {
            // Remove hologram and re-add without hologram
            manager().remove(id);
            manager().create(id, config.statKey(), config.period(), null, config.icon());
            ctx.sendMessage("§aHolograma do leaderboard §f" + id + " §aremovido.");
            return;
        }

        Player player = ctx.getSenderAsPlayer();
        if (player == null) throw fail("§cApenas jogadores podem definir a posição do holograma.");

        manager().setHologramLocation(id, player.getLocation());
        ctx.sendMessage("§aHolograma do leaderboard §f" + id + " §amovido para sua posição.");

        Tasks.runAsync(() -> manager().refreshAll());
    }

    // ── List ─────────────────────────────────────────────────

    private void handleList(@NotNull CommandContext ctx) {
        Collection<LeaderboardConfig> all = manager().getAll();
        if (all.isEmpty()) {
            ctx.sendMessage("§cNenhuma leaderboard encontrada.");
            return;
        }

        ctx.sendMessage("§6§lLeaderboards §7(" + all.size() + "):");
        for (LeaderboardConfig config : all) {
            String holo = config.hologramLoc() != null ? " §a[holo]" : "";
            ctx.sendMessage("§7- §f" + config.id() + " §7(§e" + config.statKey()
                + " §7/ §e" + config.period().name() + "§7)" + holo);
        }
    }

    // ── Info ─────────────────────────────────────────────────

    private void handleInfo(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 2) throw fail("§cUso: /lb info <id>");

        String id = ctx.getArgs()[1].toLowerCase();
        LeaderboardConfig config = manager().get(id);
        if (config == null) throw fail("§cLeaderboard '" + id + "' não encontrada.");

        ctx.sendMessage(
            "§6§l" + config.id(),
            "§7Stat: §f" + config.statKey(),
            "§7Período: §f" + config.period().name(),
            "§7Entradas: §f" + config.maxEntries(),
            "§7Holograma: §f" + (config.hologramLoc() != null
                ? String.format("%.1f, %.1f, %.1f", config.hologramLoc().getX(),
                    config.hologramLoc().getY(), config.hologramLoc().getZ())
                : "nenhum")
        );
    }

    // ── Reload ───────────────────────────────────────────────

    private void handleReload(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePermission(ctx, "leaderboard.admin");
        manager().unloadAll();
        manager().loadAll();
        ctx.sendMessage("§aLeaderboards recarregadas.");
        Tasks.runAsync(() -> manager().refreshAll());
    }

    // ── Refresh ──────────────────────────────────────────────

    private void handleRefresh(@NotNull CommandContext ctx) throws CommandFailedException {
        requirePermission(ctx, "leaderboard.admin");
        ctx.sendMessage("§7Atualizando dados...");
        Tasks.runAsync(() -> {
            manager().refreshAll();
            Tasks.runSync(() -> ctx.sendMessage("§aDados atualizados."));
        });
    }

    // ── Help ─────────────────────────────────────────────────

    private void sendHelp(@NotNull CommandContext ctx) {
        ctx.sendMessage(
            "§6§lLeaderboard §7— Comandos:",
            "§e/lb §7— abrir menu de rankings",
            "§e/lb <id> §7— ver um ranking no chat",
            "§e/lb menu §7— abrir menu de rankings",
            "§e/lb create <id> <stat> [period] §7— criar ranking",
            "§e/lb remove <id> §7— remover ranking",
            "§e/lb hologram <id> §7— colocar holograma",
            "§e/lb hologram <id> remove §7— remover holograma",
            "§e/lb list §7— listar rankings",
            "§e/lb info <id> §7— detalhes do ranking",
            "§e/lb reload §7— recarregar config",
            "§e/lb refresh §7— forçar atualização"
        );
    }

    // ── Tab Complete ─────────────────────────────────────────

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("menu", "create", "remove", "hologram", "list", "info", "reload", "refresh", "help"));
            subs.addAll(manager().getAll().stream().map(LeaderboardConfig::id).toList());
            return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("hologram") || sub.equals("info")) {
                return StringUtil.copyPartialMatches(args[1],
                    manager().getAll().stream().map(LeaderboardConfig::id).toList(),
                    new ArrayList<>());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return StringUtil.copyPartialMatches(args[3],
                Arrays.stream(StatPeriod.values()).map(Enum::name).toList(),
                new ArrayList<>());
        }

        return List.of();
    }

    // ── Helpers ──────────────────────────────────────────────

    private void requirePermission(@NotNull CommandContext ctx, @NotNull String perm) throws CommandFailedException {
        if (!ctx.getSender().hasPermission(perm)) {
            throw fail("§cVocê não tem permissão.");
        }
    }

    private CommandFailedException fail(String msg) {
        return new CommandFailedException(msg);
    }
}


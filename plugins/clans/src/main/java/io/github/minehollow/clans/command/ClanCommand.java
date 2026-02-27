package io.github.minehollow.clans.command;

import io.github.minehollow.clans.ClansPlugin;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.menu.ClanConfirmationMenu;
import io.github.minehollow.clans.menu.ClanMainMenu;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.model.ClanMember;
import io.github.minehollow.clans.model.ClanPermission;
import io.github.minehollow.clans.service.ClanResult;
import io.github.minehollow.clans.service.ClanService;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.wallet.WalletTransactionContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClanCommand extends SimpleCommand {

    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private static final List<String> SUB_COMMANDS = List.of(
        "menu", "criar", "desfazer", "convidar", "entrar", "sair",
        "expulsar", "info", "transferir", "fogoamigo", "melhorar",
        "permissao", "chat", "reload"
    );

    private final ClansPlugin plugin;
    private final ClanService service;

    public ClanCommand(@NotNull ClansPlugin plugin) {
        super("clan");
        this.plugin = plugin;
        this.service = plugin.getClanService();
        this.playersOnly = true;
    }

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null) {
            handleMenu(ctx.getSenderAsPlayer());
            return;
        }

        Player player = ctx.getSenderAsPlayer();

        switch (sub.toLowerCase()) {
            case "criar", "create" -> handleCreate(ctx, player);
            case "desfazer", "disband" -> handleDisband(player);
            case "convidar", "invite" -> handleInvite(ctx, player);
            case "entrar", "join" -> handleJoin(ctx, player);
            case "sair", "leave" -> handleLeave(player);
            case "expulsar", "kick" -> handleKick(ctx, player);
            case "info", "informacao", "informações", "information" -> handleInfo(ctx, player);
            case "transferir", "transfer" -> handleTransfer(ctx, player);
            case "fogoamigo", "friendlyfire", "ff" -> handleFriendlyFire(player);
            case "melhorar", "upgrade" -> handleUpgrade(player);
            case "permissao", "permissão", "perm", "permission" -> handlePermission(ctx, player);
            case "chat", "c" -> handleChat(ctx, player);
            case "menu", "gui" -> handleMenu(player);
            case "reload" -> handleReload(player);
            default -> handleMenu(player);
        }
    }

    // ═══════════════════════════════════════
    //  SUB-COMMANDS
    // ═══════════════════════════════════════

    private void handleCreate(@NotNull CommandContext ctx, @NotNull Player player) {
        String tag = ctx.getRawArgOrThrow(1, msg().get("usage.create"));
        String name = ctx.getRawArgOrThrow(2, msg().get("usage.create"));

        int minTag = plugin.getConfig().getInt("tag.min-length", 2);
        int maxTag = plugin.getConfig().getInt("tag.max-length", 5);
        int minName = plugin.getConfig().getInt("name.min-length", 3);
        int maxName = plugin.getConfig().getInt("name.max-length", 20);

        if (!TAG_PATTERN.matcher(tag).matches() || tag.length() < minTag || tag.length() > maxTag) {
            player.sendMessage(ClanResult.INVALID_TAG.getMessage());
            return;
        }
        if (name.length() < minName || name.length() > maxName) {
            player.sendMessage(ClanResult.INVALID_NAME.getMessage());
            return;
        }

        if (plugin.getClanService().getByName(name) != null) {
            player.sendMessage(ClanResult.NAME_TAKEN.getMessage());
            return;
        }

        double cost = plugin.getConfig().getDouble("creation-cost", 5000.0);
        String currencyId = plugin.getConfig().getString("currency-id", "coins");

        var walletService = BukkitPlatform.getInstance().getPlayerWalletService();
        if (!walletService.hasSufficientBalance(player.getUniqueId(), currencyId, BigDecimal.valueOf(cost))) {
            player.sendMessage(ClanResult.INSUFFICIENT_FUNDS.getMessage());
            return;
        }

        Thread.startVirtualThread(() -> {
            walletService.subtractCurrencyValue(
                player.getUniqueId(),
                currencyId,
                BigDecimal.valueOf(cost),
                WalletTransactionContext.builder().withInitiatorId(player.getUniqueId())
                    .withReason("clan_creation")
                    .build()
            );

            ClanResult result = service.create(tag, name, player.getUniqueId());
            if (result == ClanResult.SUCCESS) {
                walletService.subtractCurrencyValue(
                    player.getUniqueId(), currencyId, BigDecimal.valueOf(cost),
                    WalletTransactionContext.builder().withInitiatorId(player.getUniqueId()).withReason("clan_creation").build()
                );
                msg().sendList(player, "clan-created", "tag", tag.toUpperCase(), "name", name);
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleDisband(@NotNull Player player) {
        Map<String, Object> data = new HashMap<>();
        data.put(ClanConfirmationMenu.KEY_ACTION, ClanConfirmationMenu.ACTION_DISBAND);
        MenuUtil.openMenu(player, ClanConfirmationMenu.class, data);
    }

    private void handleInvite(@NotNull CommandContext ctx, @NotNull Player player) {
        String targetName = ctx.getRawArgOrThrow(1, msg().get("usage.invite"));
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            throw new CommandFailedException(msg().get("player-not-found"));
        }

        Thread.startVirtualThread(() -> {
            ClanResult result = service.invite(player.getUniqueId(), target.getUniqueId());
            if (result == ClanResult.SUCCESS) {
                Clan clan = service.getByPlayer(player.getUniqueId());
                String tag = clan != null ? clan.getTag() : "???";
                msg().send(player, "invite-sent", "player", target.getName());
                msg().sendList(target, "invite-received", "tag", tag);
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleJoin(@NotNull CommandContext ctx, @NotNull Player player) {
        String tag = ctx.getRawArgOrThrow(1, msg().get("usage.join"));

        Thread.startVirtualThread(() -> {
            ClanResult result = service.join(player.getUniqueId(), tag, plugin.getSlotTable());
            if (result == ClanResult.SUCCESS) {
                msg().send(player, "joined-clan", "tag", tag.toUpperCase());
                broadcastToClan(tag, msg().get("member-joined-broadcast", "player", player.getName()));
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleLeave(@NotNull Player player) {
        Thread.startVirtualThread(() -> {
            Clan clan = service.getByPlayer(player.getUniqueId());
            String tag = clan != null ? clan.getTag() : "";
            ClanResult result = service.leave(player.getUniqueId());
            if (result == ClanResult.SUCCESS) {
                msg().send(player, "left-clan");
                broadcastToClan(tag, msg().get("member-left-broadcast", "player", player.getName()));
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleKick(@NotNull CommandContext ctx, @NotNull Player player) {
        String targetName = ctx.getRawArgOrThrow(1, msg().get("usage.kick"));
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            throw new CommandFailedException(msg().get("player-not-found"));
        }

        Thread.startVirtualThread(() -> {
            ClanResult result = service.kick(player.getUniqueId(), target.getUniqueId());
            if (result == ClanResult.SUCCESS) {
                msg().send(player, "kick-success", "player", target.getName());
                msg().send(target, "kick-target");
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleInfo(@NotNull CommandContext ctx, @NotNull Player player) {
        Thread.startVirtualThread(() -> {
            Clan clan;
            String tagArg = ctx.getRawArgOrNull(1);
            if (tagArg != null) {
                clan = service.getByTag(tagArg);
            } else {
                clan = service.getByPlayer(player.getUniqueId());
            }

            if (clan == null) {
                player.sendMessage(ClanResult.CLAN_NOT_FOUND.getMessage());
                return;
            }

            int maxMembers = clan.getMaxMembers(plugin.getSlotTable());
            String ownerName = resolvePlayerName(clan.getOwnerId());
            String ffStatus = clan.isFriendlyFire()
                              ? msg().get("friendly-fire-enabled")
                              : msg().get("friendly-fire-disabled");

            msg().sendList(
                player, "info",
                "tag", clan.getTag(),
                "name", clan.getName(),
                "owner", ownerName,
                "members", clan.getMembers().size(),
                "max_members", maxMembers,
                "tier", clan.getSlotTier(),
                "friendly_fire", ffStatus
            );
        });
    }

    private void handleTransfer(@NotNull CommandContext ctx, @NotNull Player player) {
        String targetName = ctx.getRawArgOrThrow(1, msg().get("usage.transfer"));
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            throw new CommandFailedException(msg().get("player-not-found"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put(ClanConfirmationMenu.KEY_ACTION, ClanConfirmationMenu.ACTION_TRANSFER);
        data.put(ClanConfirmationMenu.KEY_TRANSFER_TARGET, target.getUniqueId());
        MenuUtil.openMenu(player, ClanConfirmationMenu.class, data);
    }

    private void handleFriendlyFire(@NotNull Player player) {
        Thread.startVirtualThread(() -> {
            ClanResult result = service.toggleFriendlyFire(player.getUniqueId());
            if (result == ClanResult.SUCCESS) {
                Clan clan = service.getByPlayer(player.getUniqueId());
                boolean ff = clan != null && clan.isFriendlyFire();
                String status = ff ? msg().get("friendly-fire-enabled") : msg().get("friendly-fire-disabled");
                broadcastToClan(
                    clan != null ? clan.getTag() : "",
                    msg().get("friendly-fire-broadcast", "status", status)
                );
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleUpgrade(@NotNull Player player) {
        var config = plugin.getConfig().getConfigurationSection("upgrades.slots");
        if (config == null) {
            player.sendMessage(msg().get("no-upgrades-available"));
            return;
        }

        Thread.startVirtualThread(() -> {
            Clan clan = service.getByPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(ClanResult.NOT_IN_CLAN.getMessage());
                return;
            }

            int nextTier = clan.getSlotTier() + 1;
            int maxTier = plugin.getSlotTable().length;
            if (nextTier > maxTier) {
                player.sendMessage(ClanResult.MAX_TIER.getMessage());
                return;
            }

            double cost = plugin.getConfig().getDouble("upgrades.slots." + nextTier + ".cost", 0);
            String currencyId = plugin.getConfig().getString("currency-id", "coins");

            var walletService = BukkitPlatform.getInstance().getPlayerWalletService();
            if (!walletService.hasSufficientBalance(player.getUniqueId(), currencyId, BigDecimal.valueOf(cost))) {
                player.sendMessage(ClanResult.INSUFFICIENT_FUNDS.getMessage());
                return;
            }

            ClanResult result = service.upgradeSlots(player.getUniqueId(), maxTier);
            if (result == ClanResult.SUCCESS) {
                walletService.subtractCurrencyValue(
                    player.getUniqueId(), currencyId, BigDecimal.valueOf(cost),
                    WalletTransactionContext.builder().withInitiatorId(player.getUniqueId()).withReason("clan_upgrade").build()
                );
                int newMax = plugin.getSlotTable()[nextTier - 1];
                broadcastToClan(clan.getTag(), msg().get("upgrade-success", "slots", newMax));
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handlePermission(@NotNull CommandContext ctx, @NotNull Player player) {
        String action = ctx.getRawArgOrThrow(1, msg().get("usage.permission"));
        String targetName = ctx.getRawArgOrThrow(2, msg().get("usage.permission"));
        String permName = ctx.getRawArgOrThrow(3, msg().get("usage.permission"));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            throw new CommandFailedException(msg().get("player-not-found"));
        }

        ClanPermission perm = ClanPermission.fromName(permName);
        if (perm == null) {
            throw new CommandFailedException(msg().get("invalid-permission", "permission", permName));
        }

        Thread.startVirtualThread(() -> {
            ClanResult result = switch (action.toLowerCase()) {
                case "grant", "add" -> service.grantPermission(player.getUniqueId(), target.getUniqueId(), perm);
                case "revoke", "remove" -> service.revokePermission(player.getUniqueId(), target.getUniqueId(), perm);
                default -> throw new CommandFailedException(msg().get("use-grant-or-revoke"));
            };

            if (result == ClanResult.SUCCESS) {
                boolean granted = action.equalsIgnoreCase("grant") || action.equalsIgnoreCase("add");
                String key = granted ? "permission-granted" : "permission-revoked";
                msg().send(player, key, "permission", perm.getDisplayName(), "player", target.getName());
            } else {
                player.sendMessage(result.getMessage());
            }
        });
    }

    private void handleChat(@NotNull CommandContext ctx, @NotNull Player player) {
        if (ctx.getArgs().length < 2) {
            throw new CommandFailedException(msg().get("usage.chat"));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < ctx.getArgs().length; i++) {
            if (i > 1) {
                sb.append(' ');
            }
            sb.append(ctx.getArgs()[i]);
        }
        String message = sb.toString();

        Thread.startVirtualThread(() -> {
            Clan clan = service.getByPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(ClanResult.NOT_IN_CLAN.getMessage());
                return;
            }

            ClanMember member = clan.getMember(player.getUniqueId());
            if (member == null || !member.hasPermission(ClanPermission.CHAT)) {
                player.sendMessage(ClanResult.NO_PERMISSION.getMessage());
                return;
            }

            String formatted = msg().get("chat-format", "player", player.getName(), "message", message);
            for (ClanMember m : clan.getMembers()) {
                Player online = Bukkit.getPlayer(m.getUuid());
                if (online != null) {
                    StringUtils.send(online, formatted);
                }
            }
        });
    }

    private void handleMenu(@NotNull Player player) {
        Thread.startVirtualThread(() -> {
            Clan clan = service.getByPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(ClanResult.NOT_IN_CLAN.getMessage());
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> MenuUtil.openMenu(player, ClanMainMenu.class));
        });
    }

    private void handleReload(@NotNull Player player) {
        if (!player.hasPermission("clan.admin")) {
            player.sendMessage(ClanResult.NO_PERMISSION.getMessage());
            return;
        }
        plugin.reloadAll();
        player.sendMessage(msg().get("reload-success"));
    }

    // ═══════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════

    private void broadcastToClan(@NotNull String tag, @NotNull String message) {
        Clan clan = service.getByTag(tag);
        if (clan == null) {
            return;
        }

        for (ClanMember m : clan.getMembers()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null) {
                StringUtils.send(online, message);
            }
        }
    }

    private @NotNull String resolvePlayerName(@NotNull java.util.UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }

    private void sendHelp(@NotNull Player player) {
        msg().sendList(player, "help");
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            final String start = ctx.getArgs()[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String cmd : SUB_COMMANDS) {
                if (cmd.startsWith(start)) {
                    suggestions.add(cmd);
                }
            }
            for (String alias : List.of(
                "create", "disband", "invite", "join", "leave", "kick", "transfer",
                "friendlyfire", "ff", "upgrade", "permission", "perm", "gui"
            )) {
                if (alias.startsWith(start)) {
                    suggestions.add(alias);
                }
            }
            return suggestions;
        }

        String sub = ctx.getArgs()[0].toLowerCase();

        if (ctx.getArgs().length == 2) {
            return switch (sub) {
                case "convidar", "invite", "expulsar", "kick", "transferir", "transfer" -> NONE_ARGS;
                case "entrar", "join" -> List.of("<tag>");
                case "permissao", "permissão", "permission", "perm" -> List.of("grant", "revoke");
                default -> NONE_ARGS;
            };
        }

        if (ctx.getArgs().length == 3 && (sub.equals("permissao") || sub.equals("permissão") || sub.equals("permission") || sub.equals("perm"))) {
            return NONE_ARGS;
        }

        if (ctx.getArgs().length == 4 && (sub.equals("permissao") || sub.equals("permissão") || sub.equals("permission") || sub.equals("perm"))) {
            List<String> perms = new ArrayList<>();
            for (ClanPermission p : ClanPermission.values()) {
                if (p.name().toLowerCase().startsWith(ctx.getArgs()[3].toLowerCase())) {
                    perms.add(p.name().toLowerCase());
                }
            }
            return perms;
        }

        return NONE_ARGS;
    }
}


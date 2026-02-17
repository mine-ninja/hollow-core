package io.github.minehollow.mailbox.command;

import io.github.minehollow.mailbox.MailboxPlugin;
import io.github.minehollow.mailbox.model.MailboxItem;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MailboxAdminCommand extends SimpleCommand {
    private final MailboxPlugin plugin;

    public MailboxAdminCommand(@NotNull MailboxPlugin plugin) {
        super("correio-admin", "mailbox.admin");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            ctx.sendMessage(
                    "§e/correio-admin send <player|@a> <descrição> §7- Envia item na mão",
                    "§e/correio-admin list <player> §7- Lista entregas pendentes");
            return;
        }

        switch (subCommand.toLowerCase()) {
            case "send" -> handleSend(ctx);
            case "list" -> handleList(ctx);
            default ->
                throw new CommandFailedException("§cSub-comando inválido. Use /correio-admin para ver os comandos.");
        }
    }

    private void handleSend(@NotNull CommandContext ctx) {
        Player sender = ctx.getSenderAsPlayer();

        String target = ctx.getRawArgOrThrow(1, "§cUse: /correio-admin send <player|@a> <descrição>");
        if (ctx.getArgs().length < 3) {
            throw new CommandFailedException("§cUse: /correio-admin send <player|@a> <descrição>");
        }

        String description = ctx.joinArgs(2);

        ItemStack handItem = sender.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            throw new CommandFailedException("§cVocê precisa estar segurando um item na mão.");
        }

        ItemStack[] items = new ItemStack[] { handItem.clone() };

        if ("@a".equalsIgnoreCase(target)) {
            var onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) {
                throw new CommandFailedException("§cNenhum jogador online.");
            }

            for (Player player : onlinePlayers) {
                MailboxItem box = MailboxItem.create("Admin", description, items, handItem.getType().name());
                plugin.getMailboxService().sendToPlayer(player.getUniqueId(), box);
            }

            StringUtils.send(sender,
                    "<green>Enviado para <white>" + onlinePlayers.size() + " <green>jogadores: <gray>" + description);
        } else {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                throw new CommandFailedException("§cJogador '" + target + "' não está online.");
            }

            MailboxItem box = MailboxItem.create("Admin", description, items, handItem.getType().name());
            plugin.getMailboxService().sendToPlayer(targetPlayer.getUniqueId(), box);

            StringUtils.send(sender,
                    "<green>Enviado para <white>" + targetPlayer.getName() + "<green>: <gray>" + description);
        }
    }

    private void handleList(@NotNull CommandContext ctx) {
        Player targetPlayer = ctx.getLocalPlayerOrThrow(1, "§cUse: /correio-admin list <player>");

        Thread.startVirtualThread(() -> {
            var data = plugin.getMailboxService().loadOrCreate(targetPlayer.getUniqueId());

            if (data.getBoxes().isEmpty()) {
                ctx.sendMessage("§c" + targetPlayer.getName() + " não tem entregas pendentes.");
                return;
            }

            ctx.sendMessage("§e§lCorreio de " + targetPlayer.getName() + " (" + data.getPendingCount() + "):");
            for (var box : data.getBoxes()) {
                ctx.sendMessage("§7 - §f" + box.getDescription()
                        + " §7| De: " + box.getSenderPlugin()
                        + " §7| Itens: " + box.getItemCount());
            }
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            return filterStartingWith(List.of("send", "list"), ctx.getRawArgOrNull(0));
        }

        if (ctx.getArgs().length == 2) {
            String sub = ctx.getRawArgOrNull(0);
            if ("send".equalsIgnoreCase(sub)) {
                var names = new java.util.ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).toList());
                names.add("@a");
                return filterStartingWith(names, ctx.getRawArgOrNull(1));
            }
            if ("list".equalsIgnoreCase(sub)) {
                return filterStartingWith(
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                        ctx.getRawArgOrNull(1));
            }
        }

        return NONE_ARGS;
    }
}

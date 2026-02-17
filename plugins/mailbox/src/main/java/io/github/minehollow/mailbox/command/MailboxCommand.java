package io.github.minehollow.mailbox.command;

import io.github.minehollow.mailbox.MailboxPlugin;
import io.github.minehollow.mailbox.menu.MailboxMenu;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MailboxCommand extends SimpleCommand {
    private final MailboxPlugin plugin;

    public MailboxCommand(@NotNull MailboxPlugin plugin) {
        super("correio");
        this.setAliases(List.of("resgatar"));
        this.playersOnly = true;
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();

        var cached = plugin.getMailboxService().getCachedData(player.getUniqueId());
        if (cached != null) {
            MenuUtil.openMenu(player, MailboxMenu.class);
            return;
        }

        StringUtils.send(player, "<gray>Carregando correio...");
        Tasks.runAsync(() -> {
            plugin.getMailboxService().loadOrCreate(player.getUniqueId());
            Tasks.runSync(() -> {
                if (player.isOnline()) {
                    MenuUtil.openMenu(player, MailboxMenu.class);
                }
            });
        });
    }
}

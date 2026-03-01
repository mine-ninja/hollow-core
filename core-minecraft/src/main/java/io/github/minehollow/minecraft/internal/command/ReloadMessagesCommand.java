package io.github.minehollow.minecraft.internal.command;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadMessagesCommand extends SimpleCommand {
    private final BukkitPlatform platform;

    public ReloadMessagesCommand(@NotNull BukkitPlatform platform) {
        super("reloadmessages", "hollow.admin");
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.getSender() instanceof Player player && !player.hasPermission("hollow.reloadmessages")) {
            throw new CommandFailedException("<red>Você não tem permissão para recarregar as mensagens.");
        }
        platform.getMessageConfig().reload();
        ctx.sendMessage("<green>Mensagens recarregadas com sucesso!");
    }
}


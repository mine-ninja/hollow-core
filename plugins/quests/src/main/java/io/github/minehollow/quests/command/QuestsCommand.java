package io.github.minehollow.quests.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.quests.menu.QuestListMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuestsCommand extends SimpleCommand {
    public QuestsCommand() {
        super("quests");
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        MenuUtil.openMenu(player, QuestListMenu.class);
    }
}

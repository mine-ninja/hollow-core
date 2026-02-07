package io.github.minehollow.ranks.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.menu.edit.RewardListMenu;
import org.jetbrains.annotations.NotNull;

public class RankRewardEditCommand extends SimpleCommand {
    private final RanksPlugin plugin;

    public RankRewardEditCommand(RanksPlugin plugin) {
        super("rewardedit");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        if (!player.hasPermission("ranks.admin")) {
            player.sendMessage(StringUtils.text("<red>Você não tem permissão para usar este comando!"));
            return;
        }

        MenuUtil.openMenu(player, RewardListMenu.class);
    }
}
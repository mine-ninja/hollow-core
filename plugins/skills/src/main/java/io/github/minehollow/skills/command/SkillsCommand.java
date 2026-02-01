package io.github.minehollow.skills.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.menu.SkillsMainMenu;
import org.jetbrains.annotations.NotNull;

import static io.github.minehollow.minecraft.menu.MenuUtil.openMenu;


public class SkillsCommand extends SimpleCommand {

    private final SkillsPlugin plugin;

    public SkillsCommand(@NotNull SkillsPlugin plugin) {
        super("skills");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        openMenu(player, SkillsMainMenu.class);
    }
}

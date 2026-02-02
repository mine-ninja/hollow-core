package io.github.minehollow.kits.command.subcommand;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.menu.KitEditorMenu;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;

import java.util.List;
import java.util.Map;

public class EditSubCommand extends SimpleSubCommand {
    private final KitService kitService;
    private final BukkitPlatform platform;

    public EditSubCommand(KitService kitService, BukkitPlatform platform) {
        super("edit");
        this.kitService = kitService;
        this.platform = platform;
        this.permission = "kit.admin";
        this.playersOnly = true;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 2) {
            throw new CommandFailedException("Uso: /kit edit <nome>");
        }

        String kitName = ctx.getArgs()[1];
        Kit kit = kitService.findKitByName(kitName);

        if (kit == null) {
            throw new CommandFailedException("Kit não encontrado: " + kitName);
        }

        platform.getMenuManager().openToPlayer(ctx.getSenderAsPlayer(), KitEditorMenu.class, Map.of("kit", kit));
    }

    @Override
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 2) {
            return filterStartingWith(kitService.getKitNames(), ctx.getArgs()[1]);
        }
        return List.of();
    }
}

package io.github.minehollow.kits.command.subcommand;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.message.StringUtils;

import java.util.List;

public class DeleteSubCommand extends SimpleSubCommand {
    private final KitService kitService;

    public DeleteSubCommand(KitService kitService) {
        super("delete");
        this.kitService = kitService;
        this.permission = "kit.admin";
        this.playersOnly = false;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 2) {
            throw new CommandFailedException("Uso: /kit delete <nome>");
        }

        String kitName = ctx.getArgs()[1];
        Kit kit = kitService.findKitByName(kitName);

        if (kit == null) {
            throw new CommandFailedException("Kit não encontrado: " + kitName);
        }

        kitService.deleteKit(kit.getId()).thenAccept(v -> ctx.getSender().sendMessage(StringUtils.text(
            "<green>Kit <white>" + kit.getDisplayName() + " <green>deletado com sucesso!"))).exceptionally(ex -> {
            ctx.getSender().sendMessage(StringUtils.text("<red>Erro ao deletar: " + ex.getMessage()));
            return null;
        });
    }

    @Override
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 2) {
            return filterStartingWith(kitService.getKitNames(), ctx.getArgs()[1]);
        }
        return List.of();
    }
}

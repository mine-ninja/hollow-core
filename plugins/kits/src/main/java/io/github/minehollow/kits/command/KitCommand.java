package io.github.minehollow.kits.command;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.command.subcommand.*;
import io.github.minehollow.kits.menu.KitCategoryMenu;
import io.github.minehollow.kits.menu.KitListMenu;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class KitCommand extends SimpleCommand {
    private final KitService kitService;
    private final BukkitPlatform platform;
    private final List<SimpleSubCommand> subCommands = new ArrayList<>();

    public KitCommand(KitService kitService, BukkitPlatform platform) {
        super("kit");
        setAliases(List.of("kits"));
        this.kitService = kitService;
        this.platform = platform;

        subCommands.add(new CategorySubCommand(kitService, platform));
        subCommands.add(new CreateSubCommand(kitService, platform));
        subCommands.add(new DeleteSubCommand(kitService));
        subCommands.add(new EditSubCommand(kitService, platform));
        subCommands.add(new GiveSubCommand(kitService));
        subCommands.add(new PreviewSubCommand(kitService, platform));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.isArgsLength(0)) {
            Player player = ctx.getSenderAsPlayer();
            if (kitService.getAllCategories().isEmpty()) {
                platform.getMenuManager().openToPlayer(player, KitListMenu.class);
            } else {
                platform.getMenuManager().openToPlayer(player, KitCategoryMenu.class);
            }
            return;
        }

        String arg = ctx.getArgs()[0].toLowerCase();

        for (SimpleSubCommand sub : subCommands) {
            if (sub.matchesWith(arg)) {
                sub.handleSubCommand(ctx);
                return;
            }
        }

        if (arg.equals("help")) {
            showHelp(ctx);
            return;
        }

        handleUseKit(ctx, arg);
    }

    private void handleUseKit(CommandContext ctx, String kitName) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        Kit kit = kitService.findKitByName(kitName);

        if (kit == null) {
            throw new CommandFailedException("Kit não encontrado: " + kitName);
        }

        if (!kitService.hasPermission(player, kit)) {
            throw new CommandFailedException("Você não tem permissão para usar este kit!");
        }

        kitService.claimKit(player, kit).thenAccept(result -> {
            switch (result.status()) {
                case SUCCESS -> player.sendMessage(StringUtils.text(
                        "<green>Você coletou o kit <white>" + kit.getDisplayName() + "<green>!"));
                case ON_COOLDOWN -> player.sendMessage(StringUtils.text(
                        "<gray>Este kit estará disponível em: <yellow>" + result.getFormattedRemainingTime()));
                case NO_SPACE -> player.sendMessage(StringUtils.text(
                        "<red>Você não tem espaço suficiente no inventário!"));
                case NO_PERMISSION -> player.sendMessage(StringUtils.text(
                        "<red>Você não tem permissão para usar este kit!"));
                case NOT_FOUND -> player.sendMessage(StringUtils.text(
                        "<red>Kit não encontrado!"));
            }
        });
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(StringUtils.text("<gradient:#E0AAFF:#9D4EDD>Kit Commands"));
        ctx.sendMessage(StringUtils.text("<white>/kit <gray>- Abre o menu de kits"));
        ctx.sendMessage(StringUtils.text("<white>/kit <nome> <gray>- Usa um kit"));
        ctx.sendMessage(StringUtils.text("<white>/kit preview <nome> <gray>- Visualiza um kit"));

        if (ctx.getSender().hasPermission("kit.admin")) {
            ctx.sendMessage("");
            ctx.sendMessage(StringUtils.text("<gradient:#E0AAFF:#9D4EDD>Admin Commands"));
            ctx.sendMessage(StringUtils.text("<white>/kit category <gray>- Gerenciar categorias"));
            ctx.sendMessage(
                    StringUtils.text("<white>/kit create <id> <categoria> <cooldown> <gray>- Cria um novo kit"));
            ctx.sendMessage(StringUtils.text("<white>/kit edit <nome> <gray>- Edita um kit"));
            ctx.sendMessage(StringUtils.text("<white>/kit delete <nome> <gray>- Deleta um kit"));
            ctx.sendMessage(StringUtils.text("<white>/kit give <jogador> <kit> <gray>- Dá um kit a um jogador"));
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = ctx.getRawArgOrNull(0);

            for (SimpleSubCommand sub : subCommands) {
                if (sub.hasPermission(ctx.getSender())) {
                    suggestions.addAll(sub.getAllNames());
                }
            }
            suggestions.add("help");

            suggestions.addAll(kitService.getKitNames());

            return filterStartingWith(suggestions, prefix == null ? "" : prefix);
        }

        if (ctx.getArgs().length >= 2) {
            String arg = ctx.getArgs()[0].toLowerCase();
            for (SimpleSubCommand sub : subCommands) {
                if (sub.matchesWith(arg) && sub.hasPermission(ctx.getSender())) {
                    return sub.performSubCommandTabComplete(ctx);
                }
            }
        }

        return NONE_ARGS;
    }
}

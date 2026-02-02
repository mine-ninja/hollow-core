package io.github.minehollow.kits.command.subcommand;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class GiveSubCommand extends SimpleSubCommand {
    private final KitService kitService;

    public GiveSubCommand(KitService kitService) {
        super("give");
        this.kitService = kitService;
        this.permission = "kit.admin";
        this.playersOnly = false;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 3) {
            throw new CommandFailedException("Uso: /kit give <jogador> <kit>");
        }

        String playerName = ctx.getArgs()[1];
        String kitName = ctx.getArgs()[2];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            throw new CommandFailedException("Jogador não encontrado: " + playerName);
        }

        Kit kit = kitService.findKitByName(kitName);
        if (kit == null) {
            throw new CommandFailedException("Kit não encontrado: " + kitName);
        }

        var result = kitService.giveKit(target, kit);
        switch (result) {
            case SUCCESS -> {
                ctx.getSender().sendMessage(StringUtils.text(
                    "<green>Kit <white>" + kit.getDisplayName() + " <green>dado para <white>" + target.getName()
                        + "<green>!"));
                target.sendMessage(StringUtils.text(
                    "<green>Você recebeu o kit <white>" + kit.getDisplayName() + "<green>!"));
            }
            case NO_SPACE -> throw new CommandFailedException(target.getName() + " não tem espaço no inventário!");
            default -> throw new CommandFailedException("Erro ao dar o kit!");
        }
    }

    @Override
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 2) {
            return filterStartingWith(
                Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                ctx.getArgs()[1]);
        }
        if (ctx.getArgs().length == 3) {
            return filterStartingWith(kitService.getKitNames(), ctx.getArgs()[2]);
        }
        return List.of();
    }
}

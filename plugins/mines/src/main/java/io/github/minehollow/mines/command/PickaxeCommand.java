package io.github.minehollow.mines.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.mines.pickaxe.PickaxeManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PickaxeCommand extends SimpleCommand {

    public PickaxeCommand() {
        super("picareta" , "mines.pickaxe");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var sender = ctx.getSender();
        if (!(sender instanceof Player player)) {
            throw new CommandFailedException("Apenas jogadores podem usar este comando.");
        }

        if (hasPickaxeInInventory(player)) {
            throw new CommandFailedException("Você já tem uma picareta em seu inventário.");
        }

        final var pickaxe = PickaxeManager.createPickaxe(1);
        player.getInventory().addItem(pickaxe);
        player.sendMessage(StringUtils.formatString("<green>Você recebeu uma picareta!"));
    }


    private boolean hasPickaxeInInventory(@NotNull Player player) {
        final var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            final var item = contents[i];
            if (item == null) {
                continue;
            }
            if (PickaxeManager.isPickaxe(item)) {
                return true;
            }
        }

        return false;
    }
}
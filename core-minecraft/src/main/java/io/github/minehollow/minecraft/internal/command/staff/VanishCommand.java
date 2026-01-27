package io.github.minehollow.minecraft.internal.command.staff;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class VanishCommand extends SimpleCommand {

    private static final String VANISH_MESSAGE = "§dVocê entrou no vanish com sucesso.";
    private static final String UNVANISH_MESSAGE = "§dVocê saiu do vanish com sucesso.";

    @SuppressWarnings("unused")
    private final BukkitPlatform platform;

    public VanishCommand(BukkitPlatform platform) {
        super("v", "hollow.vanish");
        this.setAliases(List.of("vanish"));
        this.platform = platform;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        final var manager = BukkitPlatform.getInstance().getVanishManager();

        if (manager.isVanished(player)) {
            manager.unvanish(player);
            player.sendMessage(UNVANISH_MESSAGE);
        } else {
            manager.vanish(player);
            player.sendMessage(VANISH_MESSAGE);
        }
    }
}

package net.warcane.lugin.core.minigames.internal.command.party;

import net.warcane.lugin.core.MinecraftServerPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PartyChatCommand extends SimpleCommand {
    private final MinecraftServerPlatform platform;

    public PartyChatCommand(MinecraftServerPlatform platform) {
        super("p");
        setAliases(List.of("partychat", "partyc", "pc"));
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {

    }
}

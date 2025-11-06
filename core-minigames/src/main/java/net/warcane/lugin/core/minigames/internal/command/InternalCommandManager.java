package net.warcane.lugin.core.minigames.internal.command;

import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.internal.command.party.PartyChatCommand;
import net.warcane.lugin.core.minigames.internal.command.party.PartyCommand;
import org.bukkit.Bukkit;

public record InternalCommandManager(MinigamesPlatform platform) {

    public void registerInternalCommands() {
        var commandMap = Bukkit.getCommandMap();
        commandMap.register("lugin", new PartyCommand(platform));
        commandMap.register("lugin", new PartyChatCommand(platform));
    }
}

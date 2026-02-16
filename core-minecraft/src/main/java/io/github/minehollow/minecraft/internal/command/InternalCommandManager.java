package io.github.minehollow.minecraft.internal.command;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.currency.Currency;
import io.github.minehollow.minecraft.gamerule.command.GameRuleCommand;
import io.github.minehollow.minecraft.internal.command.currency.CurrencyBasedCommand;
import io.github.minehollow.minecraft.internal.command.currency.EconomyCommand;
import io.github.minehollow.minecraft.internal.command.discord.LinkCommand;
import io.github.minehollow.minecraft.internal.command.discord.UnlinkCommand;
import io.github.minehollow.minecraft.internal.command.gamemode.GameModeCommand;
import io.github.minehollow.minecraft.internal.command.lobby.LobbyCommand;
import io.github.minehollow.minecraft.internal.command.server.ServerCommand;
import io.github.minehollow.minecraft.internal.command.staff.*;
import io.github.minehollow.minecraft.internal.command.test.TestMenuCommand;
import io.github.minehollow.minecraft.internal.command.test.TestTimeCommand;
import io.github.minehollow.minecraft.util.commands.TextTestCommand;
import io.github.minehollow.sdk.util.property.Property;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.jetbrains.annotations.NotNull;

/**
 * Representa um gerenciador de comandos internos na plataforma Bukkit.
 * Esta classe é responsável por gerenciar comandos que são utilizados internamente pelo plugin
 * como permissões, grupos, throwable outros comandos administrativos.
 */
@RequiredArgsConstructor
public class InternalCommandManager {

    private final BukkitPlatform platform;

    /**
     * Registra os comandos internos na plataforma Bukkit.
     * Este method deve ser chamado durante a inicialização do plugin para garantir que os comandos
     * estejam disponíveis para uso.
     */
    public void registerInternalCommands() {
        CommandMap commandMap = Bukkit.getCommandMap();
        commandMap.register("hollow", new GameModeCommand());

        final var lobbyEnabled = Property.get("LOBBY_ENABLED", "true").equalsIgnoreCase("true");
        if (lobbyEnabled) {
            commandMap.register("hollow", new LobbyCommand(platform));
        }

        commandMap.register("hollow", new StaffMessageCommand(platform));
        commandMap.register("hollow", new PlayerInfoCommand(platform));
        commandMap.register("hollow", new ServerCommand(platform));
        commandMap.register("hollow", new TestMenuCommand(platform));
        commandMap.register("hollow", new TestTimeCommand());
        commandMap.register("hollow", new VanishCommand(platform));
        commandMap.register("hollow", new GoCommand(platform));
        commandMap.register("hollow", new GameRuleCommand(platform));
        commandMap.register("hollow", new TextTestCommand());
        commandMap.register("hollow", new LinkCommand(platform));
        commandMap.register("hollow", new UnlinkCommand(platform));
        commandMap.register("hollow", new EconomyCommand(platform));
    }

    public void registerCurrencyCommand(@NotNull Currency currency) {
        final var command = new CurrencyBasedCommand(platform, currency, currency.allowPlayerPayments());
        Bukkit.getCommandMap().register("hollow", command);
    }
}

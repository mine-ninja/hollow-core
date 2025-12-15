package net.warcane.lugin.core.minecraft.internal.command;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.currency.Currency;
import net.warcane.lugin.core.minecraft.gamerule.command.GameRuleCommand;
import net.warcane.lugin.core.minecraft.internal.command.currency.CurrencyBasedCommand;
import net.warcane.lugin.core.minecraft.internal.command.currency.EconomyCommand;
import net.warcane.lugin.core.minecraft.internal.command.discord.LinkCommand;
import net.warcane.lugin.core.minecraft.internal.command.discord.UnlinkCommand;
import net.warcane.lugin.core.minecraft.internal.command.gamemode.GameModeCommand;
import net.warcane.lugin.core.minecraft.internal.command.lobby.LobbyCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.GroupPermissionCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.PlayerGroupCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.PlayerPermissionCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.TestPermission;
import net.warcane.lugin.core.minecraft.internal.command.server.ServerCommand;
import net.warcane.lugin.core.minecraft.internal.command.staff.*;
import net.warcane.lugin.core.minecraft.internal.command.test.TestMenuCommand;
import net.warcane.lugin.core.minecraft.internal.command.test.TestTimeCommand;
import net.warcane.lugin.core.minecraft.punish.command.CheckPunishCommand;
import net.warcane.lugin.core.minecraft.punish.command.PunishCommand;
import net.warcane.lugin.core.minecraft.punish.command.RevokeCommand;
import net.warcane.lugin.core.minecraft.tell.ReplyCommand;
import net.warcane.lugin.core.minecraft.tell.TellCommand;
import net.warcane.lugin.core.minecraft.tell.ToggleTellCommand;
import net.warcane.lugin.core.minecraft.util.commands.TextTestCommand;
import net.warcane.lugin.core.util.property.Property;
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
        commandMap.register("lugin", new GroupPermissionCommand(platform));
        commandMap.register("lugin", new TestPermission());
        commandMap.register("lugin", new PlayerGroupCommand(platform));
        commandMap.register("lugin", new GameModeCommand());
        commandMap.register("lugin", new PlayerPermissionCommand(platform));

        final var lobbyEnabled = Property.get("LOBBY_ENABLED", "true").equalsIgnoreCase("true");
        if (lobbyEnabled) {
            commandMap.register("lugin", new LobbyCommand(platform));
        }
        commandMap.register("lugin", new StaffMessageCommand(platform));
        commandMap.register("lugin", new PlayerInfoCommand(platform));
        commandMap.register("lugin" , new ServerCommand(platform));
        commandMap.register("lugin", new EconomyCommand(platform));
        commandMap.register("lugin", new TestMenuCommand(platform));
        commandMap.register("lugin", new TestTimeCommand());
        commandMap.register("lugin", new VanishCommand(platform));
        commandMap.register("lugin", new GoCommand(platform));
        commandMap.register("lugin", new StaffCommand(platform));
        commandMap.register("punish", new PunishCommand());
        commandMap.register("punish", new CheckPunishCommand());
        commandMap.register("punish", new RevokeCommand());
        commandMap.register("lugin", new GameRuleCommand(platform));
        commandMap.register("lugin", new TextTestCommand());
        commandMap.register("lugin", new LinkCommand(platform));
        commandMap.register("lugin", new UnlinkCommand(platform));

        commandMap.register("lugin", new TellCommand(platform));
        commandMap.register("lugin", new ReplyCommand(platform));
        commandMap.register("lugin", new ToggleTellCommand(platform));
    }

    public void registerCurrencyCommand(@NotNull Currency currency) {
        Bukkit.getCommandMap().register("lugin", new CurrencyBasedCommand(platform, currency , currency.allowPlayerPayments()));
    }
}

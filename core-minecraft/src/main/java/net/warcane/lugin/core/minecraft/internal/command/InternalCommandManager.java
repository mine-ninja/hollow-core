package net.warcane.lugin.core.minecraft.internal.command;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.internal.command.lobby.LobbyCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.GroupPermissionCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.PlayerGroupCommand;
import net.warcane.lugin.core.minecraft.internal.command.permission.TestPermission;
import net.warcane.lugin.core.minecraft.internal.command.server.ServerCommand;
import net.warcane.lugin.core.minecraft.internal.command.staff.PlayerInfoCommand;
import net.warcane.lugin.core.minecraft.internal.command.staff.StaffMessageCommand;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

/**
 * Representa um gerenciador de comandos internos na plataforma Bukkit.
 * Esta classe é responsável por gerenciar comandos que são utilizados internamente pelo plugin
 * como permissões, grupos, e outros comandos administrativos.
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

        final var lobbyEnabled = Property.get("LOBBY_ENABLED", "false").equalsIgnoreCase("true");
        if (lobbyEnabled) {
            commandMap.register("lugin", new LobbyCommand(platform));
        }
        commandMap.register("lugin", new StaffMessageCommand(platform));
        commandMap.register("lugin", new PlayerInfoCommand(platform));
        commandMap.register("lugin" , new ServerCommand());
    }
}

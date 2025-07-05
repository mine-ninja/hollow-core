package net.warcane.lugin.core.minecraft.internal.command.permission;

import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.group.GroupPermissionSet;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

public class GroupPermissionCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    private final GroupPermissionService service;

    public GroupPermissionCommand(BukkitPlatform platform) {
        super("groupperm");
        this.platform = platform;
        this.service = platform.getGroupPermissionService();
        this.requiredPermission = "lugin.master";
    }


    // /groupperm add <group> <permission>
    // /groupperm remove <group> <permission>
    // /groupperm list <group>
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrThrow(0, "§cVocê deve especificar um subcomando: add, remove ou list.");
        final var group = ctx.getEnumOrThrow(1, PlayerGroup.class, "§cGrupo inválido. Use um dos seguintes: " + String.join(", ", PlayerGroup.NAMES));

        final var groupPermissions = service.getCachedPermissionsForGroup(group);
        if (groupPermissions == null) {
            throw new CommandFailedException("§cPermissões do grupo " + group.name() + " não encontradas.");
        }

        switch (subCommand.toLowerCase()) {
            case "add" -> handleAddCommand(ctx, group, groupPermissions);
            case "remove" -> handleRemoveCommand(ctx, group, groupPermissions);
            case "list" -> handleListCommand(ctx, group, groupPermissions);
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: add, remove ou list.");
        }
    }

    private void handleAddCommand(@NotNull CommandContext ctx, @NotNull PlayerGroup group, @NotNull GroupPermissionSet currentPermissions) throws CommandFailedException {
        final var permissionToAdd = ctx.getRawArgOrThrow(2, "§cVocê deve especificar uma permissão para adicionar.");
        if (currentPermissions.hasPermission(permissionToAdd)) {
            throw new CommandFailedException("§cA permissão " + permissionToAdd + " já está presente no grupo " + group.name() + ".");
        }

        service.updateGroupPermissionSet(currentPermissions.addPermission(permissionToAdd)).whenComplete((updated, error) -> {
            if (error != null) {
                ctx.sendMessage("§cErro ao adicionar permissão: " + error.getMessage());
            } else if (updated != null) {
                ctx.sendMessage("§aPermissão " + permissionToAdd + " adicionada ao grupo " + group.name() + ".");
            } else {
                ctx.sendMessage("§cErro incomum ao adicionar permissão ao grupo " + group.name() + ".");
            }
        });
    }

    private void handleRemoveCommand(@NotNull CommandContext ctx, @NotNull PlayerGroup group, @NotNull GroupPermissionSet currentPermissions) throws CommandFailedException {
        final var permission = ctx.getRawArgOrThrow(2, "§cVocê deve especificar uma permissão para remover.");
        if (!currentPermissions.hasPermission(permission)) {
            throw new CommandFailedException("§cA permissão " + permission + " não está presente no grupo " + group.name() + ".");
        }

        service.updateGroupPermissionSet(currentPermissions.removePermission(permission)).whenComplete((updated, error) -> {
            if (error != null) {
                ctx.sendMessage("§cErro ao remover permissão: " + error.getMessage());
            } else if (updated != null) {
                ctx.sendMessage("§aPermissão " + permission + " removida do grupo " + group.name() + ".");
            } else {
                ctx.sendMessage("§cErro incomum ao remover permissão do grupo " + group.name() + ".");
            }
        });
    }

    private void handleListCommand(@NotNull CommandContext ctx, @NotNull PlayerGroup group, @NotNull GroupPermissionSet currentPermissions) throws CommandFailedException {
        int size = currentPermissions.permissions().size();
        if (size == 0) {
            ctx.sendMessage("§cO grupo " + group.name() + " não possui permissões definidas.");
            return;
        }

        final var joinedPermissions = String.join(", ", currentPermissions.permissions());
        ctx.sendMessage("§aPermissões do grupo " + group.name() + " §7(" + size + "):" + joinedPermissions);
    }
}

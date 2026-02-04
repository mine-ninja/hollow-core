package io.github.minehollow.minecraft.internal.command.permission;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.sdk.group.GroupPermissionService;
import io.github.minehollow.sdk.group.GroupPermissionSet;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GroupPermissionCommand extends SimpleCommand {

    private final GroupPermissionService service;

    public GroupPermissionCommand() {
        super("groupperm", "hollow.master");
        this.service = BukkitPlatform.getInstance().getGroupPermissionService();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrThrow(0, "§cVocê deve especificar um subcomando: add, remove ou list.");
        final var group = ctx.getArgOrThrow(1, PlayerGroup::fromId, "§cGrupo inválido. Use um dos seguintes: " + String.join(", ", PlayerGroup.NAMES));

        service.getGroupPermissionSet(group)
            .whenComplete((found, error) -> {
                if (error != null) {
                    log.error("Erro ao carregar permissões do grupo {}: {}", group.name(), error.getMessage());
                } else if (found == null) {
                    ctx.sendMessage("§cGrupo " + group.name() + " não encontrado ou sem permissões definidas.");
                } else {
                    switch (subCommand.toLowerCase()) {
                        case "add" -> handleAddCommand(ctx, group, found);
                        case "remove" -> handleRemoveCommand(ctx, group, found);
                        case "list" -> handleListCommand(ctx, group, found);
                        default -> throw new CommandFailedException("§cSubcomando inválido. Use: add, remove ou list.");
                    }
                }
            });
    }

    private void handleAddCommand(@NotNull CommandContext ctx, @NotNull PlayerGroup group, @NotNull GroupPermissionSet currentPermissions) throws CommandFailedException {
        var cachedPermissionsForGroup = service.getCachedPermissionsForGroup(group);
        if (cachedPermissionsForGroup != null) {
            log.debug("Permissões em cache para o grupo {}: {}", group.name(), cachedPermissionsForGroup);
        }

        final var permissionToAdd = ctx.getRawArgOrThrow(2, "§cVocê deve especificar uma permissão para adicionar.");
        if (currentPermissions.hasPermission(permissionToAdd)) {
            throw new CommandFailedException("§cA permissão " + permissionToAdd + " já está presente no grupo " + group.name() + ".");
        }

        service.updateGroupPermissionSet(currentPermissions.addPermission(permissionToAdd))
            .whenComplete((updated, error) -> {
                if (error != null) {
                    ctx.sendMessage("§cErro ao adicionar permissão: " + error.getMessage());
                } else {
                    ctx.sendMessage("§aPermissão " + permissionToAdd + " adicionada ao grupo " + group.name() + ".");
                }
            });
    }

    private void handleRemoveCommand(@NotNull CommandContext ctx, @NotNull PlayerGroup group, @NotNull GroupPermissionSet currentPermissions) throws CommandFailedException {
        final var permission = ctx.getRawArgOrThrow(2, "§cVocê deve especificar uma permissão para remover.");
        if (!currentPermissions.hasPermission(permission)) {
            throw new CommandFailedException("§cA permissão " + permission + " não está presente no grupo " + group.name() + ".");
        }

        service.updateGroupPermissionSet(currentPermissions.removePermission(permission))
            .whenComplete((updated, error) -> {
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
        log.debug("Listando permissões do grupo: {} {}", group.name(), currentPermissions);

        int size = currentPermissions.permissions().size();
        if (size == 0) {
            log.debug("Grupo {} não possui permissões definidas.", group.name());
            log.debug("GroupPermissionSet: {}", currentPermissions);
            ctx.sendMessage("§cO grupo " + group.name() + " não possui permissões definidas.");
            return;
        }

        final var joinedPermissions = String.join("\n- ", currentPermissions.permissions());
        ctx.sendMessage("§aPermissões do grupo " + group.name() + " §7(" + size + "):\n- " + joinedPermissions);
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();
        final var argsLength = args.length;

        if (argsLength == 1) {
            ArrayList<String> result = Lists.newArrayList("add", "remove", "list");
            result.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
            return result;
        }

        if (argsLength == 2) {
            return PlayerGroup.NAMES.stream()
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }

        return super.performTabComplete(ctx);
    }
}

package net.warcane.lugin.core.minecraft.permission;

import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.minecraft.util.permission.PermissionGraph;
import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

final class PlayerAccountPermissible extends PermissibleBase {

    private final Player player;
    private final PlayerAccount account;
    private final GroupPermissionService groupPermissionService;

    public PlayerAccountPermissible(Player player, PlayerAccount account, GroupPermissionService groupPermissionService) {
        super(player);
        this.player = player;
        this.account = account;
        this.groupPermissionService = groupPermissionService;
    }

    @Override
    public boolean hasPermission(String inName) {
        final var group = account.currentSubscription().group();
        final var permissionGraph = groupPermissionService.getCachedPermissionsForGroupOrThrow(group);

        final PermissionGraph graph = PermissionGraph.getInstance();
        final var highest = graph.findHighestPermissionNode(inName);
        if (highest != null && permissionGraph.hasPermission(highest)) return true;

        for (var subPermission : graph.getHigherPermissions(inName)) {
            if (permissionGraph.hasPermission(subPermission)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public PlayerAccount getAccount() {
        return account;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public GroupPermissionService getGroupPermissionService() {
        return groupPermissionService;
    }
}

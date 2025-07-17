package net.warcane.lugin.core.minecraft.permission;

import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.minecraft.util.permission.PermissionGraph;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
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
    public boolean hasPermission(@NotNull String inName) {

        for (PlayerGroupSubscription subscription : account.getSubscriptions()) {
            final var group = subscription.group();
            final var permissionSet = groupPermissionService.getCachedPermissionsForGroupOrThrow(group);
            if (permissionSet.hasPermission(inName) || permissionSet.hasPermission("*")) return true;

            final PermissionGraph graph = PermissionGraph.getInstance();
            final var highest = graph.findHighestPermissionNode(inName);
            if (highest != null && permissionSet.hasPermission(highest)) return true;

            for (var subPermission : graph.getHigherPermissions(inName)) {
                if (permissionSet.hasPermission(subPermission)) {
                    return true;
                }
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

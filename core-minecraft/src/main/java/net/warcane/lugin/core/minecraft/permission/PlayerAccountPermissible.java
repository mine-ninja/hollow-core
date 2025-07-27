package net.warcane.lugin.core.minecraft.permission;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.util.permission.PermissionGraph;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

@Slf4j
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
        final var subscriptionCategory = BukkitPlatform.getInstance().getSubscriptionCategoryType();
        return checkPlayerPermission(inName, subscriptionCategory);
    }


    private boolean checkPlayerPermission(
      @NotNull String inName,
      @NotNull SubscriptionCategoryType subscriptionCategory
    ) {
        log.info("Checking permission '{}' for player '{}' in category '{}'",
          inName, player.getName(), subscriptionCategory.name());


        for (PlayerGroupSubscription subscription : account.getSubscriptions(SubscriptionCategoryType.GLOBAL)) {
            final var group = subscription.group();

            final var permissionSet = groupPermissionService.getCachedPermissionsForGroupOrThrow(group);
            if (permissionSet.hasPermission(inName) || permissionSet.hasPermission("*")) {
                log.info("Permission '{}' granted by group '{}'", inName, group.name());
                return true;
            }

            final PermissionGraph graph = PermissionGraph.getInstance();
            final var highest = graph.findHighestPermissionNode(inName);
            if (highest != null && permissionSet.hasPermission(highest)) {
                log.info("Permission '{}' granted by highest permission '{}'", inName, highest);
                return true;
            }

            for (var subPermission : graph.getHigherPermissions(inName)) {
                if (permissionSet.hasPermission(subPermission)) {
                    log.info("Permission '{}' granted by sub-permission '{}'", inName, subPermission);
                    return true;
                }
            }
        }

        log.info("Permission '{}' not granted for player '{}'", inName, player.getName());
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

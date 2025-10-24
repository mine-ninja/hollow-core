package net.warcane.lugin.core.minecraft.permission;

import net.warcane.lugin.core.group.GroupPermissionService;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.permissions.PlayerPermission;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

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
    public boolean hasPermission(@NotNull Permission perm) {
        return hasPermission(perm.getName().toLowerCase());
    }
    
    @Override
    public boolean hasPermission(@NotNull String inName) {
        final var subscriptionCategory = BukkitPlatform.getInstance().getSubscriptionCategoryType();
        return checkPlayerPermission(inName, subscriptionCategory);
    }
    
    @Override
    public synchronized @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> effectivePermissions = super.getEffectivePermissions();
        
        for (PlayerPermission playerPermission : account.getPermissions()) {
            if (!playerPermission.isExpired()) {
                effectivePermissions.add(new PermissionAttachmentInfo(player, playerPermission.permission(), null, true));
            }
        }
        
        for (PlayerGroupSubscription subscription : account.getSubscriptions(SubscriptionCategoryType.GLOBAL)) {
            final var group = subscription.group();
            final var permissionSet = groupPermissionService.getCachedPermissionsForGroupOrThrow(group);
            for (String permission : permissionSet.permissions()) {
                effectivePermissions.add(new PermissionAttachmentInfo(player, permission, null, true));
            }
        }
        
        return effectivePermissions;
    }
    
    private boolean checkPlayerPermission(@NotNull String permission, @NotNull SubscriptionCategoryType type) {
        return account.hasPermission(permission);
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

package io.github.minehollow.minecraft.compat;

import net.milkbowl.vault.permission.Permission;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.player.subscription.PlayerGroupSubscription;
import io.github.minehollow.sdk.player.subscription.SubscriptionCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class VaultCompat extends Permission {
    
    public static void register(JavaPlugin plugin) {
        Bukkit.getServicesManager().register(Permission.class, new VaultCompat(), plugin, org.bukkit.plugin.ServicePriority.High);
    }
    
    @Override
    public String getName() {
        return "hollowCore";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public String getPrimaryGroup(String worldName, String player) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            PlayerAccount account = BukkitPlatform.getInstance().getPlayerAccountService().getCachedAccountByName(player);
            if (account != null) {
                return account.getHighestSubscription().group().getId();
            }
        }
        return "default";
    }
    
    @Override
    public String[] getPlayerGroups(String worldName, String player) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            PlayerAccount account = BukkitPlatform.getInstance().getPlayerAccountService().getCachedAccountByName(player);
            if (account != null) {
                List<PlayerGroupSubscription> subscriptions = account.getSubscriptions(SubscriptionCategoryType.GLOBAL);
                return subscriptions.stream().map(s -> s.group().name()).toArray(String[]::new);
            }
        }
        return new String[0];
    }
    
    @Override
    public String[] getGroups() {
        return PlayerGroup.NAMES.toArray(new String[0]);
    }
    
    @Override
    public boolean hasGroupSupport() {
        return true;
    }
    
    @Override public boolean hasSuperPermsCompat() { return false; }
    @Override public boolean playerHas(String s, String s1, String s2) { return false; }
    @Override public boolean playerAdd(String s, String s1, String s2) { return false; }
    @Override public boolean playerRemove(String s, String s1, String s2) { return false; }
    @Override public boolean groupHas(String s, String s1, String s2) { return false; }
    @Override public boolean groupAdd(String s, String s1, String s2) { return false; }
    @Override public boolean groupRemove(String s, String s1, String s2) { return false; }
    @Override public boolean playerInGroup(String s, String s1, String s2) { return false; }
    @Override public boolean playerAddGroup(String s, String s1, String s2) { return false; }
    @Override public boolean playerRemoveGroup(String s, String s1, String s2) { return false; }
}

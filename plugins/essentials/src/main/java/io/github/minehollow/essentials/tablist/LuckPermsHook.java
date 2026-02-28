package io.github.minehollow.essentials.tablist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

/**
 * Reflection-based LuckPerms hook.
 * This class uses reflection so it compiles without the LuckPerms API on the classpath.
 * If LuckPerms is not present at runtime, the constructor will throw.
 */
public class LuckPermsHook {

    private final Object luckPermsApi;
    private final MethodHandle getUserManager;
    private final MethodHandle getUser;
    private final MethodHandle getCachedData;
    private final MethodHandle getMetaData;
    private final MethodHandle getPrefix;
    private final MethodHandle getSuffix;

    // Group weight resolution
    private final MethodHandle getGroupManager;
    private final MethodHandle getGroup;
    private final MethodHandle getGroupWeight;
    private final MethodHandle getPrimaryGroup;

    public LuckPermsHook() throws Exception {
        // Resolve LuckPerms API via Bukkit services
        Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
        var registration = Bukkit.getServicesManager().getRegistration(lpClass);
        if (registration == null) {
            throw new IllegalStateException("LuckPerms service not registered");
        }
        this.luckPermsApi = registration.getProvider();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        // LuckPerms -> UserManager
        Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
        this.getUserManager = lookup.findVirtual(lpClass, "getUserManager", MethodType.methodType(userManagerClass));

        // UserManager -> User (getUser(UUID))
        Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
        this.getUser = lookup.findVirtual(userManagerClass, "getUser", MethodType.methodType(userClass, UUID.class));

        // User -> CachedDataManager
        Class<?> cachedDataClass = Class.forName("net.luckperms.api.cacheddata.CachedDataManager");
        this.getCachedData = lookup.findVirtual(userClass, "getCachedData", MethodType.methodType(cachedDataClass));

        // CachedDataManager -> CachedMetaData
        Class<?> metaDataClass = Class.forName("net.luckperms.api.cacheddata.CachedMetaData");
        this.getMetaData = lookup.findVirtual(cachedDataClass, "getMetaData", MethodType.methodType(metaDataClass));

        // CachedMetaData -> getPrefix() / getSuffix()
        this.getPrefix = lookup.findVirtual(metaDataClass, "getPrefix", MethodType.methodType(String.class));
        this.getSuffix = lookup.findVirtual(metaDataClass, "getSuffix", MethodType.methodType(String.class));

        // User -> getPrimaryGroup() : String
        this.getPrimaryGroup = lookup.findVirtual(userClass, "getPrimaryGroup", MethodType.methodType(String.class));

        // LuckPerms -> GroupManager
        Class<?> groupManagerClass = Class.forName("net.luckperms.api.model.group.GroupManager");
        this.getGroupManager = lookup.findVirtual(lpClass, "getGroupManager", MethodType.methodType(groupManagerClass));

        // GroupManager -> getGroup(String) : Group
        Class<?> groupClass = Class.forName("net.luckperms.api.model.group.Group");
        this.getGroup = lookup.findVirtual(groupManagerClass, "getGroup", MethodType.methodType(groupClass, String.class));

        // Group -> getWeight() : OptionalInt
        this.getGroupWeight = lookup.findVirtual(groupClass, "getWeight", MethodType.methodType(java.util.OptionalInt.class));
    }

    public @Nullable String getPrefix(@NotNull Player player) {
        try {
            Object userManager = getUserManager.invoke(luckPermsApi);
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) return null;
            Object cachedData = getCachedData.invoke(user);
            Object metaData = getMetaData.invoke(cachedData);
            return (String) getPrefix.invoke(metaData);
        } catch (Throwable e) {
            return null;
        }
    }

    public @Nullable String getSuffix(@NotNull Player player) {
        try {
            Object userManager = getUserManager.invoke(luckPermsApi);
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) return null;
            Object cachedData = getCachedData.invoke(user);
            Object metaData = getMetaData.invoke(cachedData);
            return (String) getSuffix.invoke(metaData);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Returns the weight of the player's primary group, or -1 if unavailable.
     */
    public int getWeight(@NotNull Player player) {
        try {
            Object userManager = getUserManager.invoke(luckPermsApi);
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) return -1;

            String primaryGroup = (String) getPrimaryGroup.invoke(user);
            if (primaryGroup == null) return -1;

            Object groupManager = getGroupManager.invoke(luckPermsApi);
            Object group = getGroup.invoke(groupManager, primaryGroup);
            if (group == null) return -1;

            java.util.OptionalInt weight = (java.util.OptionalInt) getGroupWeight.invoke(group);
            return weight.orElse(0);
        } catch (Throwable e) {
            return -1;
        }
    }
}


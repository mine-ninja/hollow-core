package net.warcane.lugin.core.permission;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PermissionService {

    void loadGroupPermissions(@NotNull PlayerGroup group);

    List<String> getGroupPermissions(@NotNull PlayerGroup group);

    void addGroupPermission(@NotNull PlayerGroup group, @NotNull String permission);
}

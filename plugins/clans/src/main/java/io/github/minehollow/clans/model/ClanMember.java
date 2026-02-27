package io.github.minehollow.clans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A member entry stored inside a {@link Clan} document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClanMember {

    private UUID uuid;
    private List<String> permissions;

    public static @NotNull ClanMember createOwner(@NotNull UUID uuid) {
        List<String> all = new ArrayList<>();
        for (ClanPermission p : ClanPermission.values()) {
            all.add(p.name());
        }
        return new ClanMember(uuid, all);
    }

    public static @NotNull ClanMember createDefault(@NotNull UUID uuid) {
        List<String> defaults = new ArrayList<>();
        defaults.add(ClanPermission.CHAT.name());
        return new ClanMember(uuid, defaults);
    }

    public boolean hasPermission(@NotNull ClanPermission permission) {
        return permissions.contains(permission.name());
    }

    public void grantPermission(@NotNull ClanPermission permission) {
        if (!permissions.contains(permission.name())) {
            permissions.add(permission.name());
        }
    }

    public void revokePermission(@NotNull ClanPermission permission) {
        permissions.remove(permission.name());
    }
}


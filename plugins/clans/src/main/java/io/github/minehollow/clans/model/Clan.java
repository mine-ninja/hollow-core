package io.github.minehollow.clans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root document persisted in MongoDB.
 * One document per clan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clan {

    @BsonId
    private String tag;

    private String name;
    private UUID ownerId;
    private List<ClanMember> members;
    private List<UUID> pendingInvites;
    private boolean friendlyFire;
    private int slotTier;
    private Instant createdAt;

    // ── Factory ──

    public static @NotNull Clan create(@NotNull String tag, @NotNull String name, @NotNull UUID ownerId) {
        List<ClanMember> members = new ArrayList<>();
        members.add(ClanMember.createOwner(ownerId));
        return new Clan(
            tag.toUpperCase(),
            name,
            ownerId,
            members,
            new ArrayList<>(),
            false,
            1,
            Instant.now()
        );
    }

    // ── Member helpers ──

    public @Nullable ClanMember getMember(@NotNull UUID uuid) {
        for (ClanMember m : members) {
            if (m.getUuid().equals(uuid)) return m;
        }
        return null;
    }

    public boolean isMember(@NotNull UUID uuid) {
        return getMember(uuid) != null;
    }

    public boolean isOwner(@NotNull UUID uuid) {
        return ownerId.equals(uuid);
    }

    public boolean addMember(@NotNull UUID uuid) {
        if (isMember(uuid)) return false;
        members.add(ClanMember.createDefault(uuid));
        pendingInvites.remove(uuid);
        return true;
    }

    public boolean removeMember(@NotNull UUID uuid) {
        return members.removeIf(m -> m.getUuid().equals(uuid));
    }

    // ── Invite helpers ──

    public boolean hasPendingInvite(@NotNull UUID uuid) {
        return pendingInvites.contains(uuid);
    }

    public boolean addInvite(@NotNull UUID uuid) {
        if (hasPendingInvite(uuid) || isMember(uuid)) return false;
        return pendingInvites.add(uuid);
    }

    public boolean removeInvite(@NotNull UUID uuid) {
        return pendingInvites.remove(uuid);
    }

    // ── Capacity ──

    public int getMaxMembers(int[] slotTable) {
        if (slotTier < 1 || slotTier > slotTable.length) return slotTable[0];
        return slotTable[slotTier - 1];
    }

    public boolean isFull(int[] slotTable) {
        return members.size() >= getMaxMembers(slotTable);
    }
}

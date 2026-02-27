package io.github.minehollow.clans.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.model.ClanMember;
import io.github.minehollow.clans.model.ClanPermission;
import io.github.minehollow.clans.repository.ClanRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe clan service with Caffeine cache.
 * All mutating operations persist to MongoDB and update the cache.
 */
@Slf4j
public class ClanService {

    @Getter
    private final ClanRepository repository;

    /** tag → Clan cache */
    private final Cache<String, Clan> tagCache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    /** playerUUID → tag reverse-index */
    private final Cache<UUID, String> playerTagCache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    public ClanService() {
        this.repository = new ClanRepository();
    }

    // ═══════════════════════════════════════════════════
    //  READ
    // ═══════════════════════════════════════════════════

    public @Nullable Clan getByTag(@NotNull String tag) {
        String key = tag.toUpperCase();
        Clan cached = tagCache.getIfPresent(key);
        if (cached != null) return cached;

        Clan fromDb = repository.findByTag(key);
        if (fromDb != null) cache(fromDb);
        return fromDb;
    }

    public @Nullable Clan getByPlayer(@NotNull UUID playerId) {
        String tag = playerTagCache.getIfPresent(playerId);
        if (tag != null) {
            Clan cached = tagCache.getIfPresent(tag);
            if (cached != null) return cached;
        }

        Clan fromDb = repository.findByMember(playerId);
        if (fromDb != null) cache(fromDb);
        return fromDb;
    }

    // ═══════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult create(@NotNull String tag, @NotNull String name, @NotNull UUID ownerId) {
        if (getByPlayer(ownerId) != null) return ClanResult.ALREADY_IN_CLAN;
        if (getByTag(tag) != null) return ClanResult.TAG_TAKEN;

        Clan clan = Clan.create(tag, name, ownerId);
        repository.save(clan);
        cache(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  DISBAND
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult disband(@NotNull UUID ownerId) {
        Clan clan = getByPlayer(ownerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;
        if (!clan.isOwner(ownerId)) return ClanResult.NOT_OWNER;

        repository.delete(clan.getTag());
        evict(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  INVITE / JOIN / LEAVE / KICK
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult invite(@NotNull UUID inviterId, @NotNull UUID targetId) {
        Clan clan = getByPlayer(inviterId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;

        ClanMember inviter = clan.getMember(inviterId);
        if (inviter == null || !inviter.hasPermission(ClanPermission.MANAGE_MEMBERS)) return ClanResult.NO_PERMISSION;
        if (getByPlayer(targetId) != null) return ClanResult.TARGET_IN_CLAN;
        if (clan.hasPendingInvite(targetId)) return ClanResult.ALREADY_INVITED;

        clan.addInvite(targetId);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    public @NotNull ClanResult join(@NotNull UUID playerId, @NotNull String tag, int[] slotTable) {
        if (getByPlayer(playerId) != null) return ClanResult.ALREADY_IN_CLAN;

        Clan clan = getByTag(tag);
        if (clan == null) return ClanResult.CLAN_NOT_FOUND;
        if (!clan.hasPendingInvite(playerId)) return ClanResult.NOT_INVITED;
        if (clan.isFull(slotTable)) return ClanResult.CLAN_FULL;

        clan.addMember(playerId);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    public @NotNull ClanResult leave(@NotNull UUID playerId) {
        Clan clan = getByPlayer(playerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;
        if (clan.isOwner(playerId)) return ClanResult.OWNER_CANNOT_LEAVE;

        clan.removeMember(playerId);
        playerTagCache.invalidate(playerId);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    public @NotNull ClanResult kick(@NotNull UUID kickerId, @NotNull UUID targetId) {
        Clan clan = getByPlayer(kickerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;

        ClanMember kicker = clan.getMember(kickerId);
        if (kicker == null || !kicker.hasPermission(ClanPermission.MANAGE_MEMBERS)) return ClanResult.NO_PERMISSION;
        if (!clan.isMember(targetId)) return ClanResult.TARGET_NOT_IN_CLAN;
        if (clan.isOwner(targetId)) return ClanResult.CANNOT_KICK_OWNER;

        clan.removeMember(targetId);
        playerTagCache.invalidate(targetId);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  TRANSFER OWNERSHIP
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult transferOwnership(@NotNull UUID currentOwnerId, @NotNull UUID newOwnerId) {
        Clan clan = getByPlayer(currentOwnerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;
        if (!clan.isOwner(currentOwnerId)) return ClanResult.NOT_OWNER;
        if (!clan.isMember(newOwnerId)) return ClanResult.TARGET_NOT_IN_CLAN;

        // Grant all permissions to new owner, revoke extras from old
        ClanMember newOwnerMember = clan.getMember(newOwnerId);
        if (newOwnerMember != null) {
            for (ClanPermission p : ClanPermission.values()) newOwnerMember.grantPermission(p);
        }

        clan.setOwnerId(newOwnerId);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  FRIENDLY FIRE
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult toggleFriendlyFire(@NotNull UUID playerId) {
        Clan clan = getByPlayer(playerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;

        ClanMember member = clan.getMember(playerId);
        if (member == null || !member.hasPermission(ClanPermission.PVP_CONTROL)) return ClanResult.NO_PERMISSION;

        clan.setFriendlyFire(!clan.isFriendlyFire());
        persist(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  UPGRADE SLOTS
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult upgradeSlots(@NotNull UUID playerId, int maxTier) {
        Clan clan = getByPlayer(playerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;

        ClanMember member = clan.getMember(playerId);
        if (member == null || !member.hasPermission(ClanPermission.UPGRADES)) return ClanResult.NO_PERMISSION;
        if (clan.getSlotTier() >= maxTier) return ClanResult.MAX_TIER;

        clan.setSlotTier(clan.getSlotTier() + 1);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  PERMISSION MANAGEMENT
    // ═══════════════════════════════════════════════════

    public @NotNull ClanResult grantPermission(@NotNull UUID ownerId, @NotNull UUID targetId, @NotNull ClanPermission perm) {
        Clan clan = getByPlayer(ownerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;
        if (!clan.isOwner(ownerId)) return ClanResult.NOT_OWNER;

        ClanMember target = clan.getMember(targetId);
        if (target == null) return ClanResult.TARGET_NOT_IN_CLAN;

        target.grantPermission(perm);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    public @NotNull ClanResult revokePermission(@NotNull UUID ownerId, @NotNull UUID targetId, @NotNull ClanPermission perm) {
        Clan clan = getByPlayer(ownerId);
        if (clan == null) return ClanResult.NOT_IN_CLAN;
        if (!clan.isOwner(ownerId)) return ClanResult.NOT_OWNER;

        ClanMember target = clan.getMember(targetId);
        if (target == null) return ClanResult.TARGET_NOT_IN_CLAN;

        target.revokePermission(perm);
        persist(clan);
        return ClanResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════
    //  CACHE INTERNALS
    // ═══════════════════════════════════════════════════

    private void persist(@NotNull Clan clan) {
        repository.save(clan);
        cache(clan);
    }

    private void cache(@NotNull Clan clan) {
        tagCache.put(clan.getTag(), clan);
        for (ClanMember m : clan.getMembers()) {
            playerTagCache.put(m.getUuid(), clan.getTag());
        }
    }

    private void evict(@NotNull Clan clan) {
        tagCache.invalidate(clan.getTag());
        for (ClanMember m : clan.getMembers()) {
            playerTagCache.invalidate(m.getUuid());
        }
    }

    public void invalidateAll() {
        tagCache.invalidateAll();
        playerTagCache.invalidateAll();
    }
}


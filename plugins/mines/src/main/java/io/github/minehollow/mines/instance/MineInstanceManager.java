package io.github.minehollow.mines.instance;

import io.github.minehollow.mines.mine.MineDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MineInstanceManager {

    private final Map<UUID, MineInstance> byOwner = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerByMember = new ConcurrentHashMap<>();

    public @NotNull MineInstance createOrGet(@NotNull UUID ownerId, @NotNull String id, @NotNull MineDefinition definition, int level) {
        final MineInstance existing = this.byOwner.get(ownerId);
        if (existing != null) {
            if (!existing.getDefinition().getId().equalsIgnoreCase(definition.getId())) {
                this.disband(ownerId);
            } else {
                existing.setCurrentLevel(level);
                return existing;
            }
        }

        final long seed = ThreadLocalRandom.current().nextLong();
        final MineInstance created = new MineInstance(ownerId, definition, level, seed);
        this.byOwner.put(ownerId, created);
        this.ownerByMember.put(ownerId, ownerId);
        return created;
    }

    public @Nullable MineInstance findByOwner(@NotNull UUID ownerId) {
        return this.byOwner.get(ownerId);
    }

    public @Nullable MineInstance findByMember(@NotNull UUID playerId) {
        final UUID ownerId = this.ownerByMember.get(playerId);
        if (ownerId == null) {
            return null;
        }

        return this.byOwner.get(ownerId);
    }

    public boolean invite(@NotNull UUID ownerId, @NotNull UUID memberId) {
        final MineInstance ownerInstance = this.byOwner.get(ownerId);
        if (ownerInstance == null) {
            return false;
        }

        final MineInstance current = findByMember(memberId);
        if (current != null && !current.getOwnerId().equals(ownerId)) {
            leave(memberId);
        }

        final boolean added = ownerInstance.addMember(memberId);
        this.ownerByMember.put(memberId, ownerId);
        return added;
    }

    public boolean leave(@NotNull UUID memberId) {
        final UUID ownerId = this.ownerByMember.remove(memberId);
        if (ownerId == null) {
            return false;
        }

        final MineInstance instance = this.byOwner.get(ownerId);
        if (instance == null) {
            return false;
        }

        if (memberId.equals(ownerId)) {
            disband(ownerId);
            return true;
        }

        return instance.removeMember(memberId);
    }

    public @Nullable MineInstance disband(@NotNull UUID ownerId) {
        final MineInstance removed = this.byOwner.remove(ownerId);
        if (removed == null) {
            return null;
        }

        for (UUID uuid : removed.getMembers()) {
            this.ownerByMember.remove(uuid);
        }

        return removed;
    }

    public @NotNull Collection<MineInstance> getInstances() {
        return Collections.unmodifiableCollection(this.byOwner.values());
    }
}

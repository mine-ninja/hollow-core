package io.github.minehollow.npc.api;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Central registry for all NPCs. Accessible via {@code NpcPlugin.getRegistry()}.
 */
public interface NpcRegistry {

    /**
     * Creates and spawns a new NPC at the given location.
     */
    @NotNull Npc create(@NotNull String id, @NotNull Location location);

    /**
     * Gets an NPC by its ID, or null if not found.
     */
    @Nullable Npc get(@NotNull String id);

    /**
     * Returns all registered NPCs.
     */
    @NotNull Collection<Npc> getAll();

    /**
     * Removes and despawns an NPC.
     *
     * @return true if the NPC existed and was removed
     */
    boolean remove(@NotNull String id);

    /**
     * Gets an NPC by its PacketEvents entity ID (for click detection).
     */
    @Nullable Npc getByEntityId(int entityId);
}


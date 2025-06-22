package net.warcane.lugin.core.minecraft.npc;

import lombok.Getter;
import lombok.Setter;
import net.warcane.lugin.core.minecraft.npc.interact.NpcInteractListener;
import net.warcane.lugin.core.minecraft.npc.visibility.NpcVisibilityHandler;
import net.warcane.lugin.core.minecraft.util.EntityIdUtil;
import net.warcane.lugin.core.minecraft.util.PacketUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Npc {

    private final int entityId;
    private Location location;

    private final NpcVisibilityHandler visibilityHandler;
    private NpcInteractListener interactListener = null;

    private final Map<String, Object> metadata = new HashMap<>();

    Npc(@NotNull Location location) {
        this.entityId = EntityIdUtil.nextEntityId();
        this.location = location;
        this.visibilityHandler = new NpcVisibilityHandler(this);
    }

    @NotNull
    public World getWorld() {
        return this.location.getWorld();
    }

    public boolean isInsideChunk(int chunkX, int chunkZ) {
        int npcChunkX = this.location.getBlockX() >> 4;
        int npcChunkZ = this.location.getBlockZ() >> 4;
        return npcChunkX == chunkX && npcChunkZ == chunkZ;
    }

    public void teleport(@NotNull Location location) {
        this.location = location;
        var teleportPacket = PacketUtil.buildTeleportPacket(entityId, location);
        PacketUtil.broadcastPacket(teleportPacket, visibilityHandler::isShown);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMetadata(@NotNull String key) {
        Object raw = metadata.get(key);
        return raw == null ? null : (T) raw;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getMetadata(@NotNull String key, @NotNull T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    public <T> boolean hasMetadata(@NotNull String key) {
        return metadata.containsKey(key);
    }

    public <T> void setMetadata(@NotNull String key, @NotNull T value) {
        metadata.put(key, value);
    }
}

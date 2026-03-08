/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.util.PacketTransformationUtil;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.google.common.base.Preconditions;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Getter
public class MineChunkPacket extends MineBukkitPacket<WrapperPlayServerChunkData> {

    private static final int MAX_SECTION_SIZE = 2032 >> 4; // See https://minecraft.wiki/w/Altitude (Snapshot 21w05a)

    /**
     * Avoid recalculation, map self-destructs after sending the packet, please don't cache this class, if you need to cache the {@link MineChunkPacket}
     * consider to use weak references.
     */
    private final Map<Integer, LightData> trackedLightData = new ConcurrentHashMap<>();

    private final PacketWrapper<?>[] wrappers;

    public MineChunkPacket(@NotNull Column column, @Nullable Player player) {
        super(MinePacketType.CHUNK_DATA, new WrapperPlayServerChunkData(column), player);

        this.wrappers = PacketTransformationUtil.transform(this.getWrapper());
    }

    /**
     * We need to adapt the packet to the player because client version limitations.
     *
     * @param user The user to adapt the packet to
     */
    public void adapt(@NonNull User user) {
        // Check class docs to understand this method.
        final int sectionSize = user.getTotalWorldHeight() >> 4;

        Preconditions.checkArgument(sectionSize > 0, "Section size cannot be negative.");
        Preconditions.checkArgument(sectionSize < MAX_SECTION_SIZE, "Section size cannot be greater than %s.".formatted(MAX_SECTION_SIZE));

        final LightData cachedLightData = this.trackedLightData.get(sectionSize);
        final WrapperPlayServerChunkData wrapper = this.getChunkPacket();
        if (cachedLightData != null) {
            wrapper.setLightData(cachedLightData);
            return;
        }

        final LightData lightData = new LightData();
        final byte[] fullLightSection = new byte[2048];
        Arrays.fill(fullLightSection, (byte) 0xFF); // Full brightness

        final byte[][] fullLightArray = new byte[sectionSize][];
        final BitSet fullBitSet = new BitSet(sectionSize);
        for (int i = 0; i < sectionSize; i++) {
            fullLightArray[i] = fullLightSection;
            fullBitSet.set(i);
        }

        final BitSet emptyBitSet = new BitSet(sectionSize);

        lightData.setBlockLightArray(fullLightArray);
        lightData.setSkyLightArray(fullLightArray);
        lightData.setBlockLightCount(sectionSize);
        lightData.setSkyLightCount(sectionSize);
        lightData.setBlockLightMask(fullBitSet);
        lightData.setSkyLightMask(fullBitSet);
        lightData.setEmptyBlockLightMask(emptyBitSet);
        lightData.setEmptySkyLightMask(emptyBitSet);
        wrapper.setLightData(lightData);
    }

    public @NotNull WrapperPlayServerChunkData getChunkPacket() {
        for (final PacketWrapper<?> wrapper : this.wrappers) {
            if (wrapper instanceof WrapperPlayServerChunkData chunkData) {
                return chunkData;
            }
        }

        throw new IllegalStateException("No chunk data packet found in wrappers.");
    }
}

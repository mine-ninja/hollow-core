/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory.packet;

import com.github.retrooper.packetevents.protocol.world.chunk.storage.BitStorage;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class MineBitStorageUtil {

    public @NotNull BitStorage deepCopy(BitStorage original) {
        return new BitStorage(
            original.getBitsPerEntry(),
            original.getSize(),
            original.getData().clone()
        );
    }
}

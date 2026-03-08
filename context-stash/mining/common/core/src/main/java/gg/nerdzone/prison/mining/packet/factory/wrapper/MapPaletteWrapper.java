/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory.wrapper;

import com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette;
import java.util.HashMap;
import java.util.Map;

/**
 * A palette backed by a {@link com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette MapPalette}.
 */
public class MapPaletteWrapper implements Palette {

    private final int bits;

    private final int[] idToState;

    private final Map<Integer, Integer> stateToId = new HashMap<>();

    private int nextId;

    public MapPaletteWrapper(int bits, int nextId, int[] idToState, Map<Integer, Integer> stateToId) {
        this.bits = bits;
        this.nextId = nextId;
        this.idToState = idToState;
        this.stateToId.putAll(stateToId);
    }

    @Override
    public int size() {
        return this.nextId;
    }

    @Override
    public int stateToId(int state) {
        Integer id = this.stateToId.get(state);
        if (id == null && this.size() < this.idToState.length) {
            id = this.nextId++;
            this.idToState[id] = state;
            this.stateToId.put(state, id);
        }

        return id != null ? id : -1;
    }

    @Override
    public int idToState(int id) {
        if (id >= 0 && id < this.size()) {
            return this.idToState[id];
        }
        return 0;
    }

    @Override
    public int getBits() {
        return this.bits;
    }
}

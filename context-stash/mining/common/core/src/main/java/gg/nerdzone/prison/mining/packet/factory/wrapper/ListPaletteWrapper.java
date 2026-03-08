/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory.wrapper;

import com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette;
import lombok.Getter;

/**
 * A palette backed by a {@link com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette ListPalette}.
 */
public class ListPaletteWrapper implements Palette {

    @Getter
    private final int bits;

    private final int[] data;

    private int nextId;

    public ListPaletteWrapper(int bits, int nextId, int[] data) {
        this.bits = bits;
        this.nextId = nextId;
        this.data = data;
    }

    public int size() {
        return this.nextId;
    }

    public int stateToId(int state) {
        int id = -1;

        for (int i = 0; i < this.nextId; ++i) {
            if (this.data[i] == state) {
                id = i;
                break;
            }
        }

        if (id == -1 && this.size() < this.data.length) {
            id = this.nextId++;
            this.data[id] = state;
        }

        return id;
    }

    public int idToState(int id) {
        return id >= 0 && id < this.size() ? this.data[id] : 0;
    }

}

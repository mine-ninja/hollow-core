/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory.packet;

import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette;
import com.google.common.base.Preconditions;
import gg.nerdzone.prison.mining.packet.factory.wrapper.ListPaletteWrapper;
import gg.nerdzone.prison.mining.packet.factory.wrapper.MapPaletteWrapper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@UtilityClass
@Slf4j
public class MinePaletteUtil {

    private final VarHandle LIST_DATA_HANDLE;
    private final VarHandle ID_TO_STATE_HANDLE;
    private final VarHandle STATE_TO_ID_HANDLE;

    static {
        try {
            final MethodHandles.Lookup listLookup = MethodHandles.privateLookupIn(ListPalette.class, MethodHandles.lookup());
            final MethodHandles.Lookup mapLookup = MethodHandles.privateLookupIn(MapPalette.class, MethodHandles.lookup());

            final Field listDataField = ListPalette.class.getDeclaredField("data");
            listDataField.setAccessible(true);

            final Field idToStateField = MapPalette.class.getDeclaredField("idToState");
            idToStateField.setAccessible(true);

            final Field stateToIdField = MapPalette.class.getDeclaredField("stateToId");
            stateToIdField.setAccessible(true);

            ID_TO_STATE_HANDLE = mapLookup.unreflectVarHandle(idToStateField);
            LIST_DATA_HANDLE = listLookup.unreflectVarHandle(listDataField);
            STATE_TO_ID_HANDLE = mapLookup.unreflectVarHandle(stateToIdField);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError("Failed to initialize VarHandles for ListPalette: " + exception);
        }
    }

    public @NotNull ListPaletteWrapper deepCopy(ListPalette original) {
        Preconditions.checkNotNull(original);

        final int[] data = (int[]) LIST_DATA_HANDLE.get(original);
        return new ListPaletteWrapper(original.getBits(), original.size(), data.clone());
    }

    @SuppressWarnings("unchecked")
    public @NotNull MapPaletteWrapper deepCopyMapPalette(@NotNull MapPalette original) {
        Preconditions.checkNotNull(original);

        final int bits = original.getBits();
        final int[] idToState = ((int[]) ID_TO_STATE_HANDLE.get(original)).clone();
        final Map<Object, Integer> originalMap = (Map<Object, Integer>) STATE_TO_ID_HANDLE.get(original);

        final Map<Integer, Integer> clonedMap = new HashMap<>(originalMap.size());
        for (final Map.Entry<Object, Integer> entry : originalMap.entrySet()) {
            final Object key = entry.getKey();
            final Integer value = entry.getValue();

            if (!(key instanceof Integer intKey)) {
                log.warn("Found non-integer key in stateToId: {} ({})", key, key.getClass().getName());
                continue;
            }

            clonedMap.put(intKey, value);
        }

        return new MapPaletteWrapper(bits, original.size(), idToState, clonedMap);
    }

}

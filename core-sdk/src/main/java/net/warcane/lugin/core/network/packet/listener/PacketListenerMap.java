package net.warcane.lugin.core.network.packet.listener;

import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"rawtypes"})
public class PacketListenerMap {

    private final Map<Integer, List<PacketListener>> registeredListeners = new ConcurrentHashMap<>();

    public List<PacketListener> getListenersForPacket(int packetId) {
        return registeredListeners.getOrDefault(packetId, new ArrayList<>());
    }

    public <T extends NetworkPacket> void registerListener(Class<T> clazz, PacketListener<T> listener) {
        registeredListeners.computeIfAbsent(
          NetworkPacket.idOf(clazz),
          k -> new ArrayList<>()
        ).add(listener);
    }

    public void removeListener(PacketListener listener) {
        registeredListeners.values().forEach(listeners -> listeners.remove(listener));
    }
}

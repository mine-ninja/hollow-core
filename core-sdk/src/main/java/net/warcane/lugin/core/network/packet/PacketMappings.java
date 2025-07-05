package net.warcane.lugin.core.network.packet;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.warcane.lugin.core.network.packet.impl.internal.PingPacket;
import net.warcane.lugin.core.network.packet.impl.internal.PongPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectedToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerJoinRequestPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

final class PacketMappings {

    /**
     * Construtor privado para evitar a instanciação da classe.
     * <p>
     * Esta classe contém apenas constantes e métodos estáticos, portanto não deve ser instanciada.
     */
    PacketMappings() {
        throw new UnsupportedOperationException("Esta classe não deve ser instanciada.");
    }

    /**
     * Mapeia o ID de um pacote para a classe correspondente.
     * <p>
     * Os IDs devem ser únicos e não podem ser alterados após a definição.
     */
    static final Int2ObjectMap<Class<? extends NetworkPacket>> PACKET_CLASS_BY_ID = new Int2ObjectOpenHashMap<>(
      ofEntries(
        // internal
        entry(0, PingPacket.class),
        entry(1, PongPacket.class),

        // server lifecycle
        entry(2, ServerRegisterPacket.class),
        entry(3, ServerUnregisterPacket.class),

        // player lifecycle
        entry(4, PlayerConnectedToServerPacket.class),
        entry(5, PlayerDisconnectedFromServerPacket.class),
        entry(6, PlayerJoinRequestPacket.class)
      )
    );

    /**
     * Mapeia o ID de um pacote para a classe correspondente.
     */
    static final Object2IntMap<Class<? extends NetworkPacket>> PACKET_ID_BY_CLASS = new Object2IntOpenHashMap<>(
      PACKET_CLASS_BY_ID
        .int2ObjectEntrySet()
        .stream()
        .collect(Collectors.toMap(
          Map.Entry::getValue,
          Int2ObjectMap.Entry::getIntKey
        ))
    );
}

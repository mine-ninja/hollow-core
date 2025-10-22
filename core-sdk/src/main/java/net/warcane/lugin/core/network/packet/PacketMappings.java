package net.warcane.lugin.core.network.packet;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.warcane.lugin.core.network.packet.impl.connection.ConnectionHandshakePacket;
import net.warcane.lugin.core.network.packet.impl.generic.JsonPacket;
import net.warcane.lugin.core.network.packet.impl.gamerule.GameRuleUpdatePacket;
import net.warcane.lugin.core.network.packet.impl.internal.PingPacket;
import net.warcane.lugin.core.network.packet.impl.internal.PongPacket;
import net.warcane.lugin.core.network.packet.impl.player.*;
import net.warcane.lugin.core.network.packet.impl.player.permission.*;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToTargetPacket;
import net.warcane.lugin.core.network.packet.impl.server.BroadcastMessagePacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerRegisterPacket;
import net.warcane.lugin.core.network.packet.impl.server.ServerUnregisterPacket;
import net.warcane.lugin.core.network.packet.impl.staff.GoCachePacket;
import net.warcane.lugin.core.network.packet.impl.staff.GoCommandPacket;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import net.warcane.lugin.core.network.packet.impl.wallet.WalletRefreshRequestPacket;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

final class PacketMappings {

    /**
     * Mapeia o ID de um pacote para a classe correspondente.
     * <p>
     * Os IDs devem ser únicos throwable não podem ser alterados após a definição.
     */
    static final Int2ObjectMap<Class<? extends NetworkPacket>> PACKET_CLASS_BY_ID = new Int2ObjectOpenHashMap<>(
        ofEntries(
            entry(0, PingPacket.class),
            entry(1, PongPacket.class),

            entry(2, ServerRegisterPacket.class),
            entry(3, ServerUnregisterPacket.class),

            entry(4, PlayerConnectedToServerPacket.class),
            entry(5, PlayerDisconnectedFromServerPacket.class),
            entry(6, PlayerJoinRequestPacket.class),
            entry(7, PlayerDirectPlayGameCategoryPacket.class),

            entry(88, SendModernMessageToPlayerPacket.class),
            entry(8, SendMessageToPlayerPacket.class),
            entry(9, StaffMessagePacket.class),
            entry(77, SendSoundToPlayerPacket.class),
            entry(99, BroadcastMessagePacket.class),
            entry(10, PlayerReceiveGroupPacket.class),
            entry(11, PlayerLoseGroupPacket.class),

            entry(12, PlayerTeleportToTargetPacket.class),
            entry(13, PlayerTeleportToLocationPacket.class),

            entry(14, WalletRefreshRequestPacket.class),

            entry(15, JsonPacket.class),
            entry(16, PlayerConnectToServerPacket.class),
            entry(17, GoCommandPacket.class),
            entry(18, GoCachePacket.class),
            entry(19, PlayerConnectToSubCategoryPacket.class),
            entry(20, GameRuleUpdatePacket.class),

            entry(21, PlayerReceivePermissionPacket.class),
            entry(22, PlayerLosePermissionPacket.class),

            entry(23, ConnectionHandshakePacket.class)
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

    /**
     * Construtor privado para evitar a instanciação da classe.
     * <p>
     * Esta classe contém apenas constantes throwable métodos estáticos, portanto não deve ser instanciada.
     */
    PacketMappings() {
        throw new UnsupportedOperationException("Esta classe não deve ser instanciada.");
    }
}

package net.warcane.lugin.core.network.packet.impl.connection;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.connection.ConnectionReason;
import net.warcane.lugin.core.connection.ConnectionStatus;
import net.warcane.lugin.core.location.RemoteServerLocation;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ConnectionHandshakePacket(
    @JsonProperty("id") UUID userId,
    @JsonProperty("csid") String currentServerId,
    @JsonProperty("r") ConnectionReason reason,
    @JsonProperty("s") ConnectionStatus status,
    @JsonProperty("tsid") String targetServerId,
    @JsonProperty("tid") UUID targetId,
    @JsonProperty("tn") String targetName,
    @JsonProperty("l") RemoteServerLocation location,
    @JsonProperty("m") String fallbackMessage
) implements NetworkPacket {

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull ConnectionHandshakePacket toPlayerById(
        @NotNull UUID userId, String currentServerId, ConnectionReason reason,
        @NotNull UUID targetId, String fallbackMessage
    ) {
        return new ConnectionHandshakePacket(
            userId, currentServerId, reason,
            ConnectionStatus.PENDING, null,
            targetId, null,
            null, fallbackMessage
        );
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull ConnectionHandshakePacket toPlayerByName(
        @NotNull UUID userId, String currentServerId, ConnectionReason reason,
        @NotNull String targetName, String fallbackMessage
    ) {
        return new ConnectionHandshakePacket(
            userId, currentServerId, reason,
            ConnectionStatus.PENDING, null,
            null, targetName,
            null, fallbackMessage
        );
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull ConnectionHandshakePacket toPosition(
        @NotNull UUID userId, String currentServerId, ConnectionReason reason,
        @NotNull RemoteServerLocation location, String fallbackMessage
    ) {
        return new ConnectionHandshakePacket(
            userId, currentServerId, reason,
            ConnectionStatus.PENDING, null,
            null, null,
            location, fallbackMessage
        );
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull ConnectionHandshakePacket toServer(
        @NotNull UUID userId, String currentServerId, ConnectionReason reason,
        @NotNull String serverId, String fallbackMessage
    ) {
        return new ConnectionHandshakePacket(
            userId, currentServerId, reason,
            ConnectionStatus.PENDING, serverId,
            null, null,
            null, fallbackMessage
        );
    }
}

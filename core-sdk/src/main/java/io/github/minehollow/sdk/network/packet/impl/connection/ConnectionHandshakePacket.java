package io.github.minehollow.sdk.network.packet.impl.connection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import io.github.minehollow.sdk.connection.ConnectionReason;
import io.github.minehollow.sdk.connection.ConnectionStatus;
import io.github.minehollow.sdk.location.RemoteServerLocation;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
public class ConnectionHandshakePacket implements NetworkPacket {

    private UUID userId;
    private String currentServerId;
    private ConnectionReason reason;
    private ConnectionStatus status;

    private String targetServerId;
    private UUID targetId;
    private String targetName;
    private RemoteServerLocation location;
    private String fallbackMessage;

    @JsonCreator
    public ConnectionHandshakePacket(
        @JsonProperty("id") UUID userId, @JsonProperty("csid") String currentServerId,
        @JsonProperty("r") ConnectionReason reason, @JsonProperty("s") ConnectionStatus status,
        @JsonProperty("tsid") String targetServerId, @JsonProperty("tid") UUID targetId,
        @JsonProperty("tn") String targetName, @JsonProperty("l") RemoteServerLocation location,
        @JsonProperty("m") String fallbackMessage
    ) {
        this.userId = userId;
        this.currentServerId = currentServerId;
        this.reason = reason;
        this.status = status;
        this.targetServerId = targetServerId;
        this.targetId = targetId;
        this.targetName = targetName;
        this.location = location;
        this.fallbackMessage = fallbackMessage;
    }

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
            ConnectionStatus.PENDING, location.targetServerId(),
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

package io.github.minehollow.sdk.network.packet.impl.player.teleport;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

/**
 * Representa um pacote de teletransporte de jogador para um alvo específico na rede...
 *
 * @param playerId   Identificador único do jogador que está sendo teletransportado
 * @param targetId  Identificador único do alvo para onde o jogador será teletransportado
 *
 */
public record PlayerTeleportToTargetPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("tid") UUID targetId
) implements NetworkPacket {
}
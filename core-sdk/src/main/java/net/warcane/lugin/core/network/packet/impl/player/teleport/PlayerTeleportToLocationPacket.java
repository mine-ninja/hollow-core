package net.warcane.lugin.core.network.packet.impl.player.teleport;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.location.RemoteServerLocation;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

/**
 * Pacote de teletransporte de jogador para uma localização de servidor remoto.
 *
 * @param playerId       ID do jogador a ser teletransportado
 * @param targetLocation Localização do servidor de destino para onde o jogador será teletransportado
 */
public record PlayerTeleportToLocationPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("l") RemoteServerLocation targetLocation
) implements NetworkPacket { }
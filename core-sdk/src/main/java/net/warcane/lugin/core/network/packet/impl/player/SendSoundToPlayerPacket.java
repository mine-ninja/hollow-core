package net.warcane.lugin.core.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

/**
 * Representa um pacote de rede que é enviado para um jogador
 * para reproduzir um som específico do jogo.
 *
 * @param playerId  o UUID do jogador que receberá o som.
 * @param soundName o nome do som a ser reproduzido.
 */
public record SendSoundToPlayerPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("sound") String soundName,
  @JsonProperty("volume") float volume,
  @JsonProperty("pitch") float pitch
) implements NetworkPacket {
}

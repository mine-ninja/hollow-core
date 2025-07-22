package net.warcane.lugin.core.network.packet.impl.player.teleport;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.BitSet;
import java.util.UUID;

/**
 * Representa um pacote de teletransporte de jogador para um alvo específico na rede...
 *
 * @param playerId   Identificador único do jogador que está sendo teletransportado
 * @param targetName Nome do alvo para o qual o jogador está sendo teletransportado
 *
 */
public record PlayerTeleportToTargetPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("tn") String targetName,
  @JsonProperty("sp") BitSet metadata
) implements NetworkPacket {

    /**
     * Index para indicar que o jogador vai teleportar como um espectador.
     */
    private static final byte SPEC_METADATA_IDX = 0x01;

    public static PlayerTeleportToTargetPacket spectatorTeleport(UUID playerId, String targetName) {
        BitSet metadata = new BitSet();
        metadata.set(SPEC_METADATA_IDX); // Define o bit de espectador
        return new PlayerTeleportToTargetPacket(playerId, targetName, metadata);
    }

    public void setSpectator(boolean spectator) {
        if (spectator) {
            metadata.set(SPEC_METADATA_IDX);
        } else {
            metadata.clear(SPEC_METADATA_IDX);
        }
    }

    public boolean isSpectator() {
        return metadata.get(SPEC_METADATA_IDX);
    }
}
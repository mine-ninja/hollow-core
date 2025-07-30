package net.warcane.lugin.core.network.packet.impl.player.teleport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.UUID;

/**
 * Representa um pacote de teletransporte de jogador para um alvo específico na rede...
 *
 * @param playerId   Identificador único do jogador que está sendo teletransportado
 * @param targetId  Identificador único do alvo para onde o jogador será teletransportado
 * @param metadata   Metadados adicionais sobre o teletransporte, como se é um espectador
 *
 */
public record PlayerTeleportToTargetPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("tid") UUID targetId,
  @JsonProperty("sp") BitSet metadata
) implements NetworkPacket {

    /**
     * Index para indicar que o jogador vai teleportar como um espectador.
     */
    private static final byte SPEC_METADATA_IDX = 0x01;

    public static PlayerTeleportToTargetPacket spectatorTeleport(@NotNull UUID playerId, @NotNull UUID targetId) {
        BitSet metadata = new BitSet();
        metadata.set(SPEC_METADATA_IDX); // Define o bit de espectador
        return new PlayerTeleportToTargetPacket(playerId, targetId, metadata);
    }

    public void setSpectator(boolean spectator) {
        if (spectator) {
            metadata.set(SPEC_METADATA_IDX);
        } else {
            metadata.clear(SPEC_METADATA_IDX);
        }
    }

    @JsonIgnore
    public boolean isSpectator() {
        return metadata.get(SPEC_METADATA_IDX);
    }
}
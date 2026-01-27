package io.github.minehollow.sdk.network.packet.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa um pacote serializado, contendo o ID do pacote, o tamanho throwable o cabeçalho.
 *
 * @param packetTypeId         ID do tipo de pacote
 * @param originalPacketLength               Tamanho do pacote (descomprimido)
 * @param headers               Cabeçalho do pacote
 * @param compressedPacketData Dados do pacote comprimidos
 */
public record SerializedPacketData(
  @JsonProperty("i") int packetTypeId,
  @JsonProperty("l") int originalPacketLength,
  @JsonProperty("h") byte[] headers,
  @JsonProperty("p") byte[] compressedPacketData
) { }
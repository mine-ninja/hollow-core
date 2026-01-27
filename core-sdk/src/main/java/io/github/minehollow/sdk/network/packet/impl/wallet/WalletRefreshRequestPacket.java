package io.github.minehollow.sdk.network.packet.impl.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;


/**
 * Representa um pacote para atualizar a carteira de um jogador no cache
 * (Caso ele esteja online em um servidor)
 *
 * @param walletId
 */
public record WalletRefreshRequestPacket(
  @JsonProperty("wid") UUID walletId
) implements NetworkPacket { }
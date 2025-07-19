package net.warcane.lugin.core.network.packet.impl.player.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;

import java.util.UUID;

/**
 * Representa um pacote que é enviado para o jogador quando ele recebe um grupo de permissões.
 * Este pacote é usado para atualizar as permissões do jogador no servidor.
 */
public record PlayerReceiveGroupPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("g") PlayerGroup receivedGroup,
  @JsonProperty("c") SubscriptionCategoryType categoryType
) implements NetworkPacket { }
package net.warcane.lugin.core.network.packet.impl.player.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;

import java.util.UUID;

/**
 * Representa um pacote de rede que notifica a perda de um grupo de jogador.
 *
 * @param playerId Identificador único do jogador
 * @param group    O grupo de jogador perdido
 * @param type     O tipo de categoria de assinatura associada ao grupo perdido
 */
public record PlayerLoseGroupPacket(
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("g") PlayerGroup group,
  @JsonProperty("c") SubscriptionCategoryType type
) implements NetworkPacket { }

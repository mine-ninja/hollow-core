package net.warcane.lugin.core.player.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import org.jetbrains.annotations.Contract;

import java.time.Instant;

/**
 * Representa a assinatura de um jogador em um grupo específico.
 *
 * @param group             O grupo ao qual o jogador está inscrito
 * @param subscriptionStart Data e hora de início da assinatura
 * @param subscriptionEnd   Data e hora de término da assinatura
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerGroupSubscription(
  @JsonProperty("group") PlayerGroup group,
  @JsonProperty("subscription_start") Instant subscriptionStart,
  @JsonProperty("subscription_end") Instant subscriptionEnd
) {


    public static PlayerGroupSubscription defaultSubscription() {
        return new PlayerGroupSubscription(PlayerGroup.DEFAULT, Instant.now(), Instant.MAX);
    }


    public boolean isExpired() {
        return subscriptionEnd.isBefore(Instant.now());
    }

    @Contract(pure = true)
    public PlayerGroupSubscription changeEnd(Instant newEnd) {
        return new PlayerGroupSubscription(group, subscriptionStart, newEnd);
    }

    @Contract(pure = true)
    public PlayerGroupSubscription changeGroup(PlayerGroup newGroup) {
        return new PlayerGroupSubscription(newGroup, subscriptionStart, subscriptionEnd);
    }
}

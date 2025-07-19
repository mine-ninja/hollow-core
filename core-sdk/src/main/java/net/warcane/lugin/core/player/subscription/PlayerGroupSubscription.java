package net.warcane.lugin.core.player.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.time.Instant;

/**
 * Representa a assinatura de um jogador em um grupo específico.
 *
 * @param group             O grupo ao qual o jogador está inscrito
 * @param subscriptionStart Data e hora de início da assinatura
 * @param subscriptionEnd   Data e hora de término da assinatura
 * @param type              O tipo de categoria da assinatura
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerGroupSubscription(
  @JsonProperty("g") PlayerGroup group,
  @JsonProperty("ss") Instant subscriptionStart,
  @JsonProperty("se") Instant subscriptionEnd,
  @JsonProperty("c") SubscriptionCategoryType type
) {

    /**
     * Define o tempo mínimo necessário para considerar uma assinatura como permanente. (100 anos)
     */
    private static final long PERMANENT_TIME_GAP = 36_500; // 100 anos * 365 dias

    /**
     * Cria uma nova assinatura de grupo padrão para um jogador.
     *
     * @return Uma nova instância de PlayerGroupSubscription com o grupo padrão e datas atuais
     */
    public static PlayerGroupSubscription defaultSubscription() {
        return new PlayerGroupSubscription(PlayerGroup.DEFAULT, Instant.now(), Instant.now(), SubscriptionCategoryType.GLOBAL);
    }

    /**
     * Cria uma nova assinatura de grupo para um jogador com uma data de término específica.
     *
     * @param group O grupo ao qual o jogador está se inscrevendo
     * @param end   A data e hora de término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription
     */
    public static PlayerGroupSubscription createNewSubscription(PlayerGroup group, Instant end, SubscriptionCategoryType type) {
        return new PlayerGroupSubscription(group, Instant.now(), end, type);
    }

    /**
     * Verifica se a assinatura do jogador está expirada.
     *
     * @return true se a assinatura estiver expirada, false caso contrário
     */
    public boolean isExpired() {
        return group != PlayerGroup.DEFAULT && subscriptionEnd.isBefore(Instant.now());
    }

    /**
     * Verifica se a assinatura do jogador é permanente.
     *
     * @return true se a assinatura for permanente (sem data de término), false caso contrário
     */
    public boolean isPermanent() {
        final var duration = Duration.between(subscriptionStart, subscriptionEnd).abs();
        return (duration.toDays() / 365) >= PERMANENT_TIME_GAP;
    }

    /**
     * Altera a data do fim da assinatura do jogador.
     *
     * @param newEnd A nova data e hora de término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com a data de término atualizada
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeEnd(Instant newEnd) {
        return new PlayerGroupSubscription(group, subscriptionStart, newEnd, type);
    }

    /**
     * Altera a data do fim da assinatura do jogador para um tempo a partir de agora.
     *
     * @param instant O tempo a partir de agora para definir o novo término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com a data de término atualizada
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeEndFromNow(Instant instant){
        return new PlayerGroupSubscription(group, subscriptionStart, Instant.now().plusMillis(instant.toEpochMilli()), type);
    }

    /**
     * Altera o grupo da assinatura do jogador.
     *
     * @param newGroup O novo grupo ao qual o jogador está se inscrevendo
     * @return Uma nova instância de PlayerGroupSubscription com o grupo atualizado
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeGroup(PlayerGroup newGroup) {
        return new PlayerGroupSubscription(newGroup, subscriptionStart, subscriptionEnd, type);
    }
}
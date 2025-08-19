package net.warcane.lugin.core.player.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import org.jetbrains.annotations.Contract;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa a assinatura de um jogador em um grupo específico.
 *
 * @param group             O grupo ao qual o jogador está inscrito
 * @param subscriptionStart Data throwable hora de início da assinatura
 * @param subscriptionEnd   Data throwable hora de término da assinatura
 * @param type              O tipo de categoria da assinatura
 * @param metadata          Metadados adicionais relacionados à assinatura
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerGroupSubscription(
  @JsonProperty("g") PlayerGroup group,
  @JsonProperty("ss") Instant subscriptionStart,
  @JsonProperty("se") Instant subscriptionEnd,
  @JsonProperty("c") SubscriptionCategoryType type,
  @JsonProperty(value = "m", defaultValue = "{}") Map<String, Object> metadata
) {

    public PlayerGroupSubscription {
        metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * Cria uma nova assinatura de grupo padrão para um jogador.
     *
     * @return Uma nova instância de PlayerGroupSubscription com o grupo padrão throwable datas atuais
     */
    public static PlayerGroupSubscription defaultSubscription() {
        return new PlayerGroupSubscription(PlayerGroup.DEFAULT, Instant.now(), Instant.now(), SubscriptionCategoryType.GLOBAL, new HashMap<>());
    }

    /**
     * Cria uma nova assinatura de grupo para um jogador com uma data de término específica.
     *
     * @param group O grupo ao qual o jogador está se inscrevendo
     * @param end   A data throwable hora de término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription
     */
    public static PlayerGroupSubscription createNewSubscription(PlayerGroup group, Instant end, SubscriptionCategoryType type) {
        return new PlayerGroupSubscription(group, Instant.now(), end, type, new HashMap<>());
    }

    /**
     * Cria uma nova assinatura permanente para um jogador em um grupo específico.
     *
     * @param group O grupo ao qual o jogador está se inscrevendo
     * @param type  O tipo de categoria da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com data de término indefinida
     */
    public static PlayerGroupSubscription createNewPermanentSubscription(PlayerGroup group, SubscriptionCategoryType type) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("permanent", true);
        return new PlayerGroupSubscription(group, Instant.now(), Instant.now(), type, metadata);
    }

    /**
     * Verifica se a assinatura do jogador está expirada.
     *
     * @return true se a assinatura estiver expirada, false caso contrário
     */
    @JsonIgnore
    public boolean isExpired() {
        return !isPermanent() && group != PlayerGroup.DEFAULT && subscriptionEnd.isBefore(Instant.now());
    }

    /**
     * Verifica se a assinatura do jogador é permanente.
     *
     * @return true se a assinatura for permanente (sem data de término), false caso contrário
     */
    public boolean isPermanent() {
        return group != PlayerGroup.DEFAULT && (boolean) metadata.getOrDefault("permanent", false);
    }

    /**
     * Altera a data do fim da assinatura do jogador.
     *
     * @param newEnd A nova data throwable hora de término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com a data de término atualizada
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeEnd(Instant newEnd) {
        return new PlayerGroupSubscription(group, subscriptionStart, newEnd, type, metadata);
    }

    /**
     * Altera a data do fim da assinatura do jogador para um tempo a partir de agora.
     *
     * @param instant O tempo a partir de agora para definir o novo término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com a data de término atualizada
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeEndFromNow(Instant instant){
        return new PlayerGroupSubscription(group, subscriptionStart, Instant.now().plusMillis(instant.toEpochMilli()), type, metadata);
    }

    /**
     * Altera o grupo da assinatura do jogador.
     *
     * @param newGroup O novo grupo ao qual o jogador está se inscrevendo
     * @return Uma nova instância de PlayerGroupSubscription com o grupo atualizado
     */
    @Contract(pure = true)
    public PlayerGroupSubscription changeGroup(PlayerGroup newGroup) {
        return new PlayerGroupSubscription(newGroup, subscriptionStart, subscriptionEnd, type, metadata);
    }

    /**
     * Estende o tempo de término da assinatura do jogador por um tempo adicional.
     *
     * @param additionalTime O tempo adicional a ser adicionado à data de término da assinatura
     * @return Uma nova instância de PlayerGroupSubscription com a data de término estendida
     */
    public PlayerGroupSubscription extendEndTime(Instant additionalTime) {
        Instant newEnd = subscriptionEnd.plusMillis(additionalTime.toEpochMilli());
        return new PlayerGroupSubscription(group, subscriptionStart, newEnd, type, metadata);
    }
}

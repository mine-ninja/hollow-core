package net.warcane.lugin.core.player.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import static net.warcane.lugin.core.player.subscription.PlayerGroupSubscription.createNewSubscription;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerAccount(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("n") String playerName,
  @JsonProperty("sb") List<PlayerGroupSubscription> subscriptions,
  @JsonProperty("c") Instant createdAt,
  @JsonProperty("l") Instant lastLogin
) implements Serializable {

    /**
     * Cria uma conta de jogador padrão com o grupo "MEMBER".
     *
     * @param uniqueId   O ID único do jogador
     * @param playerName O nome do jogador
     * @return Uma nova instância de PlayerAccount com o grupo "MEMBER"
     */
    @NotNull
    public static PlayerAccount createDefaultAccount(@NotNull UUID uniqueId, @NotNull String playerName) {
        return new PlayerAccount(uniqueId, playerName, List.of(PlayerGroupSubscription.defaultSubscription()), Instant.now(), Instant.now());
    }


    /**
     * Verifica se o jogador tem poderes de um determinado grupo.
     *
     * @param group O grupo a ser verificado
     * @param type  O tipo de categoria de assinatura a ser verificado
     * @return true se o jogador tiver poderes do grupo, false caso contrário
     */
    @JsonIgnore
    public boolean hasGroupPowers(@NotNull PlayerGroup group, @NotNull SubscriptionCategoryType type) {
        return this.getSubscriptions(type)
          .stream()
          .anyMatch(subscription ->
            subscription.group().equals(group) && !subscription.isExpired() && subscription.type() == type
          );
    }


    /**
     * Obtém o nome do jogador formatado com o prefixo do grupo atual.
     *
     * @return O nome do jogador formatado
     */
    @NotNull
    @JsonIgnore
    public String getFormattedDisplayName(@NotNull SubscriptionCategoryType type) {
        return this.getFormattedTagInput(type, playerName);
    }

    @NotNull
    @JsonIgnore
    public String getFormattedTagInput(@NotNull SubscriptionCategoryType type, @NotNull String input) {
        final var currentSubscription = this.getHighestSubscription(type);
        final var primaryGroup = currentSubscription.group();
        final var groupPrefix = primaryGroup.getPrefix();
        return groupPrefix + " §" + primaryGroup.getPrefixColorCode() + input;
    }


    /**
     * Remove uma assinatura existente do jogador para um grupo throwable tipo específicos.
     *
     * @param group O grupo ao qual a assinatura pertence
     * @param type  O tipo de categoria da assinatura
     * @return Uma nova instância de PlayerAccount com a assinatura removida
     */
    public PlayerAccount removeSubscription(@NotNull PlayerGroup group, @NotNull SubscriptionCategoryType type) {
        final var currentSubscriptions = new ArrayList<>(this.subscriptions);
        final var existingSubscription = this.getSubscriptionForGroup(group, type);
        if (existingSubscription != null) {
            currentSubscriptions.remove(existingSubscription);
        }

        return new PlayerAccount(uniqueId, playerName, currentSubscriptions, createdAt, lastLogin);
    }

    /**
     * Cria uma nova assinatura para o jogador com base no grupo throwable tempo de expiração fornecidos
     * caso uma assinatura já exista para o grupo throwable tipo especificados, ela será atualizada
     * throwable seu tempo de expiração será alterado para o novo tempo fornecido.
     *
     * @param group                O grupo ao qual a assinatura pertence
     * @param targetExpirationTime O tempo de expiração da assinatura
     * @param type                 O tipo de categoria da assinatura
     * @return Uma nova instância de PlayerAccount com a assinatura atualizada
     */
    public PlayerAccount withNewSubscription(
      @NotNull PlayerGroup group,
      @NotNull Instant targetExpirationTime,
      @NotNull SubscriptionCategoryType type
    ) {
        final var currentSubscriptions = new ArrayList<>(this.subscriptions);
        final var existingSubscription = this.getSubscriptionForGroup(group, type);
        if (existingSubscription != null) {
            currentSubscriptions.remove(existingSubscription);
            currentSubscriptions.add(existingSubscription.changeEndFromNow(targetExpirationTime));
        } else {
            currentSubscriptions.add(createNewSubscription(group, targetExpirationTime, type));
        }

        return new PlayerAccount(uniqueId, playerName, currentSubscriptions, createdAt, lastLogin);
    }

    @Deprecated
    public PlayerGroupSubscription getHighestSubscription(){
        return this.getHighestSubscription(SubscriptionCategoryType.GLOBAL);
    }

    @NotNull
    @JsonIgnore
    public PlayerGroupSubscription getHighestSubscription(SubscriptionCategoryType type) {
        return this.getSubscriptions(type)
          .stream()
          .max(Comparator.comparingInt(sub -> sub.group().getPowerLevel()))
          .orElse(PlayerGroupSubscription.defaultSubscription());
    }

    @Nullable
    @JsonIgnore
    public PlayerGroupSubscription getSubscriptionForGroup(
      @NotNull PlayerGroup group,
      @NotNull SubscriptionCategoryType type
    ) {
        return this.getSubscriptions(type)
          .stream()
          .filter(subscription -> subscription.group() == group)
          .findFirst()
          .orElse(null);
    }

    @NotNull
    @JsonProperty
    public List<PlayerGroupSubscription> getSubscriptions(@NotNull SubscriptionCategoryType type) {
        if (subscriptions == null) return Collections.emptyList();

        return subscriptions.stream()
          .filter(subscription -> subscription.type() == type && !subscription.isExpired())
          .toList();
    }
}

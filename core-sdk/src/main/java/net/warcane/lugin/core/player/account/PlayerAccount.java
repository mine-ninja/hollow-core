package net.warcane.lugin.core.player.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

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
     * @return true se o jogador tiver poderes do grupo, false caso contrário
     */
    @JsonIgnore
    public boolean hasGroupPowers(@NotNull PlayerGroup group) {
        return this.getSubscriptions()
          .stream()
          .anyMatch(subscription -> subscription.group().equals(group) && !subscription.isExpired());
    }


    /**
     * Obtém o nome do jogador formatado com o prefixo do grupo atual.
     *
     * @return O nome do jogador formatado
     */
    @NotNull
    @JsonIgnore
    public String getFormattedDisplayName() {
        return this.getFormattedTagInput(playerName);
    }

    @NotNull
    @JsonIgnore
    public String getFormattedTagInput(@NotNull String input) {
        final var currentSubscription = this.getHighestSubscription();
        final var primaryGroup = currentSubscription.group();
        final var groupPrefix = primaryGroup.getPrefix();
        return groupPrefix + " §" + primaryGroup.getPrefixColorCode() + input;
    }

    @NotNull
    @Contract(pure = true)
    @JsonIgnore
    public PlayerAccount withNewGroupSubscription(@NotNull PlayerGroup newGroup, @NotNull Instant expirationTime) {
        final var list = new ArrayList<>(this.subscriptions);
        final var toAdd = PlayerGroupSubscription.createNewSubscription(newGroup, expirationTime);

        list.removeIf(subscription -> subscription.group().equals(newGroup));
        list.add(toAdd);

        return new PlayerAccount(uniqueId, playerName, list, createdAt, lastLogin);
    }

    @NotNull
    @JsonIgnore
    public PlayerGroupSubscription getHighestSubscription() {
        return this.getSubscriptions()
          .stream()
          .max(Comparator.comparingInt(sub -> sub.group().getPowerLevel()))
          .orElse(PlayerGroupSubscription.defaultSubscription());
    }

    @Nullable
    @JsonIgnore
    public PlayerGroupSubscription getSubscriptionForGroup(@NotNull PlayerGroup group) {
        return this.getSubscriptions()
          .stream()
          .filter(subscription -> subscription.group() == group)
          .findFirst()
          .orElse(null);
    }

    @NotNull
    @JsonProperty
    public List<PlayerGroupSubscription> getSubscriptions() {
        return subscriptions == null ? Collections.emptyList() : subscriptions;
    }
}

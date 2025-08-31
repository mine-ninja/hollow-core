package net.warcane.lugin.core.player.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import static net.warcane.lugin.core.player.subscription.PlayerGroupSubscription.createNewPermanentSubscription;
import static net.warcane.lugin.core.player.subscription.PlayerGroupSubscription.createNewSubscription;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerAccount(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("n") String playerName,
  @JsonProperty("sb") List<PlayerGroupSubscription> subscriptions,
  @JsonProperty("c") Instant createdAt,
  @JsonProperty("l") Instant lastLogin
) implements Serializable {


    public PlayerAccount {
        subscriptions = subscriptions == null ? new ArrayList<>() : new ArrayList<>(subscriptions);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        lastLogin = lastLogin == null ? Instant.now() : lastLogin;
    }

    /**
     * Cria uma nova instância de PlayerAccount com um novo nome.
     *
     * @param newName O novo nome do jogador
     * @return Uma nova instância de PlayerAccount com o novo nome
     */
    public PlayerAccount withNewName(@NotNull String newName) {
        return new PlayerAccount(uniqueId, newName, subscriptions, createdAt, lastLogin);
    }

    /**
     * Cria uma conta de jogador padrão com o grupo "MEMBER".
     *
     * @param uniqueId   O ID único do jogador
     * @param playerName O nome do jogador
     * @return Uma nova instância de PlayerAccount com o grupo "MEMBER"
     */
    @NotNull
    public static PlayerAccount createDefaultAccount(@NotNull UUID uniqueId, @NotNull String playerName) {
        return new PlayerAccount(uniqueId, playerName, new ArrayList<>(List.of(PlayerGroupSubscription.defaultSubscription())), Instant.now(), Instant.now());
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
        return groupPrefix + "§" + primaryGroup.getPrefixColorCode() + input;
    }

    @NotNull
    @JsonIgnore
    public Component getFormattedDisplayNameComponent(@NotNull SubscriptionCategoryType type) {
        return this.getFormattedDisplayNameComponent(type, null);
    }
    
    @NotNull
    @JsonIgnore
    public Component getFormattedDisplayNameComponent(@NotNull SubscriptionCategoryType type, @Nullable Component betweenComp) {
        final var currentSubscription = this.getHighestSubscription(type);
        final var primaryGroup = currentSubscription.group();
        final Component groupPrefix = Component.text(primaryGroup.getModernTag() == ' ' ? "" : primaryGroup.getModernTag() + " ").style(Style.style().font(Key.key("lugin:tags")).color(TextColor.color(0xFFFFFF)).build());
        final int color = switch (primaryGroup.getPrefixColorCode()) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            default -> 0xFFFFFF;
        };

        TextComponent group = Component.empty().append(groupPrefix);
        if (betweenComp != null) {
            group = group.append(betweenComp).append(Component.text(" "));
        }
        return group.append(Component.text(playerName).color(TextColor.color(color)));
    }

    public PlayerAccount removeSubscription(@NotNull PlayerGroupSubscription subscription) {
        final var currentSubscriptions = new ArrayList<>(this.subscriptions);
        currentSubscriptions.removeIf(existingSubscription -> existingSubscription.equals(subscription));

        return new PlayerAccount(uniqueId, playerName, currentSubscriptions, createdAt, lastLogin);
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

    public PlayerAccount withNewPermanentSubscription(@NotNull PlayerGroup group, @NotNull SubscriptionCategoryType type) {
        try {
            final var currentSubscriptions = subscriptions == null
              ? new ArrayList<PlayerGroupSubscription>()
              : new ArrayList<>(subscriptions);

            currentSubscriptions.removeIf(
              subscription -> Objects.equals(subscription.group(), group) && Objects.equals(subscription.type(), type));

            final var newSubscription = createNewPermanentSubscription(group, type);
            currentSubscriptions.add(newSubscription);

            return new PlayerAccount(uniqueId, playerName, currentSubscriptions, createdAt, lastLogin);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criar assinatura permanente: " + e.getMessage(), e);
        }
    }

    public PlayerAccount withNewSubscription(
      @NotNull PlayerGroup group,
      @NotNull Instant targetExpirationTime,
      @NotNull SubscriptionCategoryType type
    ) {
        try {
            final var currentSubscriptions = subscriptions == null
              ? new ArrayList<PlayerGroupSubscription>()
              : new ArrayList<>(subscriptions);

            currentSubscriptions.removeIf(
              subscription -> Objects.equals(subscription.group(), group) && Objects.equals(subscription.type(), type));

            final var newSubscription = createNewSubscription(group, targetExpirationTime, type);
            currentSubscriptions.add(newSubscription);

            return new PlayerAccount(uniqueId, playerName, currentSubscriptions, createdAt, lastLogin);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criar assinatura: " + e.getMessage(), e);
        }
    }
    
    @JsonIgnore
    public PlayerGroupSubscription getHighestSubscription() {
        return this.getHighestSubscription(SubscriptionCategoryType.GLOBAL);
    }

    @NotNull
    @JsonIgnore
    public PlayerGroupSubscription getHighestSubscription(SubscriptionCategoryType type) {
        final var highestSpecialSubscription = this.getHighestSpecialSubscription();
        return Objects.requireNonNullElseGet(highestSpecialSubscription, () -> this.getSubscriptions(type)
          .stream()
          .max(Comparator.comparingInt(sub -> sub.group().getPowerLevel()))
          .orElse(PlayerGroupSubscription.defaultSubscription())
        );
    }

    @Nullable
    @JsonIgnore
    public PlayerGroupSubscription getSubscriptionForGroup(
      @NotNull PlayerGroup group,
      @NotNull SubscriptionCategoryType type
    ) {
        return this.getSubscriptions(type)
          .stream()
          .filter(subscription -> subscription.group().equals(group))
          .findFirst()
          .orElse(null);
    }


    @NotNull
    @JsonIgnore
    public List<PlayerGroupSubscription> getSubscriptions(@NotNull SubscriptionCategoryType type) {
        if (subscriptions == null) return Collections.emptyList();

        return subscriptions.stream()
          .filter(subscription -> subscription.type() == SubscriptionCategoryType.GLOBAL) // hotfix for global fallback.
          .toList();
    }

    @Nullable
    @JsonIgnore
    public PlayerGroupSubscription getHighestSpecialSubscription() {
        return this.getSubscriptions(SubscriptionCategoryType.GLOBAL)
          .stream()
          .filter(it -> it.group().isSpecialGroup())
          .max(Comparator.comparingInt(sub -> sub.group().getPowerLevel()))
          .orElse(null);
    }


}

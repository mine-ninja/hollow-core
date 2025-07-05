package net.warcane.lugin.core.player.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerAccount(
  @JsonProperty("unique_id") UUID uniqueId,
  @JsonProperty("player_name") String playerName,
  @JsonProperty("primary_group") PlayerGroupSubscription currentSubscription,
  @JsonProperty("created_at") Instant createdAt,
  @JsonProperty("last_login") Instant lastLogin
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
        return new PlayerAccount(uniqueId, playerName, PlayerGroupSubscription.defaultSubscription(), Instant.now(), Instant.now());
    }


    /**
     * Verifica se o jogador tem poderes de um determinado grupo.
     *
     * @param group O grupo a ser verificado
     * @return true se o jogador tiver poderes do grupo, false caso contrário
     */
    @JsonIgnore
    public boolean hasGroupPowers(@NotNull PlayerGroup group) {
        return currentSubscription.group().isGreaterOrEqualTo(group);
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
        final var primaryGroup = currentSubscription.group();
        final var groupPrefix = primaryGroup.getPrefix();
        return groupPrefix + " §" + primaryGroup.getPrefixColorCode() + input;
    }

    @NotNull
    @Contract(pure = true)
    public PlayerAccount withNewGroupSubscription(@NotNull PlayerGroup newGroup, @NotNull Instant expirationTime) {
        return new PlayerAccount(uniqueId, playerName,
          currentSubscription.changeGroup(newGroup).changeEnd(expirationTime), createdAt, lastLogin);
    }
}

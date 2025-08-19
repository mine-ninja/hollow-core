package net.warcane.lugin.core.minecraft.nametag;

import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Representa um resolvedor de tags de nome para jogadores.
 * Esta interface define métodos para aplicar e remover tags de nome
 * de acordo com a conta do jogador.
 *
 * @author Sasuked (Matheus Barreto)
 */
public interface NameTagResolver {

    /**
     * Aplica a tag de nome ao jogador baseado na sua conta.
     *
     * @param account A conta do jogador para aplicar a tag de nome.
     */
    void applyNameTag(@NotNull PlayerAccount account);

    /**
     * Remove a tag de nome do jogador baseado na sua conta.
     *
     * @param account A conta do jogador para remover a tag de nome.
     */
    void removeNameTag(@NotNull PlayerAccount account);

    default @Nullable Player getPlayer(@NotNull PlayerAccount account) {
        return Bukkit.getPlayer(account.uniqueId());
    }
}

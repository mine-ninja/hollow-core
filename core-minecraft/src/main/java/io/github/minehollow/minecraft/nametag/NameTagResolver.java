package io.github.minehollow.minecraft.nametag;

import org.bukkit.entity.Player;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

/**
 * Representa um resolvedor de tags de nome para jogadores.
 * Esta interface define métodos para aplicar e remover tags de nome
 * conforme a conta do jogador.
 *
 * @author Sasuked (Matheus Barreto)
 */
public abstract class NameTagResolver {
    @Setter protected Function<Player, String> tagPrefix = (player) -> "";
    @Setter protected Function<Player, String> tagSuffix = (player) -> "";
    @Setter protected Function<Player, String> tagColor = (player) -> "";
    
    public String getTagPrefix(@NotNull Player player) {
        return this.tagPrefix.apply(player);
    }
    
    public String getTagSuffix(@NotNull Player player) {
        return this.tagSuffix.apply(player);
    }
    
    public String getTagColor(@NotNull Player player) {
        return this.tagColor.apply(player);
    }
}

package net.warcane.lugin.core.minecraft.nametag;

import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa um resolvedor de tags de nome para jogadores.
 * Esta interface define métodos para aplicar e remover tags de nome
 * de acordo com a conta do jogador.
 *
 * @author Sasuked (Matheus Barreto)
 */
public abstract class NameTagResolver {
    protected static final Map<String, NameTag> PLAYER_TAGS = new HashMap<>();
    
    @Nullable
    public String getPrefix(@NotNull Player player) {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null && existingTeam.prefix() != null) {
            return existingTeam.prefix();
        }
        return null;
    }
    
    @Nullable
    public String getSuffix(@NotNull Player player) {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null && existingTeam.suffix() != null) {
            return existingTeam.suffix();
        }
        return null;
    }
    
    @Nullable
    public String getColor(@NotNull Player player) {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null && existingTeam.color() != null) {
            return existingTeam.color();
        }
        return null;
    }
    
    public void setPrefix(@NotNull Player player, String prefix) throws UnsupportedOperationException {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null) {
            setNameTag(player, existingTeam.withPrefix(prefix));
        } else {
            throw new UnsupportedOperationException("Player does not have a name tag set.");
        }
    }
    
    public void setSuffix(@NotNull Player player, String suffix) throws UnsupportedOperationException {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null) {
            setNameTag(player, existingTeam.withSuffix(suffix));
        } else {
            throw new UnsupportedOperationException("Player does not have a name tag set.");
        }
    }
    
    public void setColor(@NotNull Player player, @Nullable String color) throws UnsupportedOperationException {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null) {
            setNameTag(player, existingTeam.withColor(color));
        } else {
            throw new UnsupportedOperationException("Player does not have a name tag set.");
        }
    }
    
    /**
     * Aplica a tag de nome ao jogador baseado na sua conta.
     *
     * @param account A conta do jogador para aplicar a tag de nome.
     */
    public abstract void applyNameTag(@NotNull PlayerAccount account);
    
    public abstract void updateAllTags();
    
    public void removeNameTag(@NotNull Player player) {
        final var team = PLAYER_TAGS.get(player.getName());
        if (team == null) { return; }
        
        PLAYER_TAGS.remove(player.getName());
    }
    
    public void setNameTag(@NotNull Player player, String prefix, String suffix, @Nullable String color) {
        NameTag existingTeam = PLAYER_TAGS.get(player.getName());
        if (existingTeam != null) {
            removeNameTag(player);
        }
        
        prefix = prefix != null ? prefix.substring(0, Math.min(prefix.length(), 16)) : "";
        suffix = suffix != null ? suffix.substring(0, Math.min(suffix.length(), 16)) : "";
        
        setNameTag(player, new NameTag(prefix, suffix, color));
    }
    
    protected void setNameTag(@NotNull Player player, NameTag team) {
        PLAYER_TAGS.put(player.getName(), team);
    }
    
    @Nullable
    protected Player getPlayer(@NotNull PlayerAccount account) {
        return Bukkit.getPlayer(account.uniqueId());
    }
    
    public record NameTag(String prefix, String suffix, @Nullable String color) {
        public NameTag withColor(@Nullable String color) {
            return new NameTag(this.prefix, this.suffix, color);
        }
        
        public NameTag withPrefix(String prefix) {
            return new NameTag(prefix, this.suffix, this.color);
        }
        
        public NameTag withSuffix(String suffix) {
            return new NameTag(this.prefix, suffix, this.color);
        }
    }
}

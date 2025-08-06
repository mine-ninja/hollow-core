package net.warcane.lugin.core.minecraft.event;

import lombok.Getter;
import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Representa um evento que é chamado quando um jogador tem sua conta carregada.
 * Este evento é disparado quando a conta do jogador é carregada do armazenamento persistente,
 *
 * @author Sasuked (Matheus Barreto)
 */
public class PlayerAccountLoadEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final PlayerAccount loadedAccount;

    public PlayerAccountLoadEvent(PlayerAccount loadedAccount) {
        super(!Bukkit.isPrimaryThread());
        this.loadedAccount = loadedAccount;
    }

    @NotNull
    public PlayerAccount getLoadedAccount() {
        return loadedAccount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}

package net.warcane.lugin.core.minecraft.event.account;

import lombok.Getter;
import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlayerNickUpdateEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final PlayerAccount playerAccount;
    private final String oldNick;
    private final String newNick;

    public PlayerNickUpdateEvent(PlayerAccount playerAccount, String oldNick, String newNick) {
        super(!Bukkit.isPrimaryThread());
        this.playerAccount = playerAccount;
        this.oldNick = oldNick;
        this.newNick = newNick;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}

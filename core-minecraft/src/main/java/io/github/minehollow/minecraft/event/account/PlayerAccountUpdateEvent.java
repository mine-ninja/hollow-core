package io.github.minehollow.minecraft.event.account;

import lombok.Getter;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerAccountUpdateEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final PlayerAccount playerAccount;

    public PlayerAccountUpdateEvent(PlayerAccount playerAccount) {
        super(!Bukkit.isPrimaryThread());
        this.playerAccount = playerAccount;
    }

    @NotNull
    public PlayerAccount getPlayerAccount() {
        return playerAccount;
    }

    @Nullable
    public Player getLocalPlayer() {
        return Bukkit.getPlayer(playerAccount.uniqueId());
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}

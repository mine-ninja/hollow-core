package net.warcane.lugin.core.minecraft.event.account;

import lombok.Getter;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.permissions.PlayerPermission;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerPermissionExpireEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final PlayerAccount account;
    private final PlayerPermission playerPermission;

    public PlayerPermissionExpireEvent(PlayerAccount account, PlayerPermission playerPermission) {
        super(!Bukkit.isPrimaryThread());

        this.account = account;
        this.playerPermission = playerPermission;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    @NotNull
    public PlayerAccount getAccount() {
        return account;
    }

    @NotNull
    public PlayerPermission getPlayerPermission() {
        return playerPermission;
    }
}

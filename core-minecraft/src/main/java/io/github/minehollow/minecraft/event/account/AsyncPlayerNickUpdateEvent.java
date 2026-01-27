package io.github.minehollow.minecraft.event.account;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncPlayerNickUpdateEvent extends Event implements Cancellable {
	private static final Component CANCELLED_MESSAGE = Component.text("<l-negate>(Código de erro: #1004)");
	@Getter
	private static final HandlerList handlerList = new HandlerList();

	private final PlayerAccount playerAccount;
	private final String oldNick;
	private final String newNick;

	public AsyncPlayerNickUpdateEvent(PlayerAccount playerAccount, String oldNick, String newNick) {
		super(!Bukkit.isPrimaryThread());
		this.playerAccount = playerAccount;
		this.oldNick = oldNick;
		this.newNick = newNick;
	}

	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	@Getter
	@Setter
	private Component canceledMessage;

	@Override
	public boolean isCancelled() {
		return canceledMessage != null;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.canceledMessage = cancel ? CANCELLED_MESSAGE : null;
	}
}

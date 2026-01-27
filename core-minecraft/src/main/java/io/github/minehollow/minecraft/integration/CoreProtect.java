package io.github.minehollow.minecraft.integration;

import org.bukkit.entity.Player;

public class CoreProtect {
	public static void log(Player player, String action) {
		net.coreprotect.CoreProtectAPI api = net.coreprotect.CoreProtect.getInstance().getAPI();
		if (!api.isEnabled()) return;
		
		api.logChat(player, action);
	}
}

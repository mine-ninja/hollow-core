package io.github.minehollow.minecraft.integration;

import org.bukkit.Bukkit;

import java.util.function.Supplier;

public enum Compat {
	CORE_PROTECT("CoreProtect");
	
	private final String id;
	
	Compat(String id) {
		this.id = id;
	}
	
	public void runIfPresent(Supplier<Runnable> action) {
		if (!Bukkit.getPluginManager().isPluginEnabled(this.id)) return;
		action.get().run();
	}
}

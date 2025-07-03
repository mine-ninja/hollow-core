package net.warcane.lugin.core.proxy;

import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class BungeecordPlatformPlugin extends Plugin {

    private BungeecordPlatform platform;

    @Override
    public void onEnable() {
        platform = new BungeecordPlatform(this);
        platform.init();
    }

    @NotNull
    public BungeecordPlatform getPlatform() {
        return platform;
    }
}

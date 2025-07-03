package net.warcane.lugin.core.minecraft;

import net.warcane.lugin.core.minecraft.plugin.SimplePlugin;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.server.type.ServerCategoryType;

public class BukkitPlatformPlugin extends SimplePlugin {

    private BukkitPlatform bukkitPlatform;

    @Override
    public void onEnable() {
        if (BukkitPlatform.isInitialized()) {
            bukkitPlatform = BukkitPlatform.getInstance();
        } else {
            bukkitPlatform = new BukkitPlatform(this, ServerCategoryType.LOBBY);
            bukkitPlatform.init(NetworkChannel.values());
        }
    }
}

package net.warcane.lugin.core.minecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;


public class Tab {

    private final String tabHeader;
    private final String tabFooter;

    public Tab(List<String> tabHeader, List<String> tabFooter) {
        this.tabHeader = String.join("\n", tabHeader);
        this.tabFooter = String.join("\n", tabFooter);
    }

    public void tick(Player p) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, getCreateHeaderFooterPacket());
    }

    private PacketWrapper<?> getCreateHeaderFooterPacket() {
        return new WrapperPlayServerPlayerListHeaderAndFooter(
          Component.text(tabHeader),
          Component.text(tabFooter)
        );
    }
}

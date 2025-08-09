package net.warcane.lugin.core.minecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;


public class Tab {

    private final Component tabHeader;
    private final Component tabFooter;

    public Tab(List<String> tabHeader, List<String> tabFooter) {
        this.tabHeader = Component.text(String.join("\n", tabHeader));
        this.tabFooter = Component.text(String.join("\n", tabFooter));
    }

    @SuppressWarnings("UnstableApiUsage")
    public Tab(List<Component> tabHeader, List<Component> tabFooter, boolean useNewline /* true */) {
        this.tabHeader = Component.join(useNewline ? Component.newline() : Component.empty(), tabHeader);
        this.tabFooter = Component.join(useNewline ? Component.newline() : Component.empty(), tabFooter);
    }

    public void tick(Player p) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, getCreateHeaderFooterPacket());
    }

    private PacketWrapper<?> getCreateHeaderFooterPacket() {
        return new WrapperPlayServerPlayerListHeaderAndFooter(
          tabHeader,
          tabFooter
        );
    }
}

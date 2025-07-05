package net.warcane.lugin.core.minecraft.util;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.entity.Player;

import java.util.List;

import static net.warcane.lugin.core.minecraft.util.ReflectionUtils.Modifier;
import static net.warcane.lugin.core.minecraft.util.ReflectionUtils.modifyClass;

public class Tab {

    private final String tabHeader;
    private final String tabFooter;

    public Tab(List<String> tabHeader, List<String> tabFooter) {
        this.tabHeader = String.join("\n", tabHeader);
        this.tabFooter = String.join("\n", tabFooter);
    }

    public void tick(Player p) {
        PacketUtil.sendPacket(p, getCreateHeaderFooterPacket());
    }

    private Packet<?> getCreateHeaderFooterPacket() {
        return modifyClass(new PacketPlayOutPlayerListHeaderFooter(),
          new Modifier<IChatBaseComponent>("a", new ChatComponentText(tabHeader)),
          new Modifier<IChatBaseComponent>("b", new ChatComponentText(tabFooter)));
    }
}

package net.warcane.lugin.core.minecraft.menu.input;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.warcane.lugin.core.minecraft.task.Tasks;

public class SignInputListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        
        SignInputContext context = SignInputMenu.remove(event.getPlayer());
        if (context == null) return;
        
        WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
        context.put("input", wrapper.getTextLines());
        context.accept();
        
        Tasks.runAtLocation(() -> {
            Vector3i pos = new Vector3i(context.getLocation().getBlockX(), context.getLocation().getBlockY(), context.getLocation().getBlockZ());
            event.getUser().sendPacket(new WrapperPlayServerBlockChange(pos, SpigotConversionUtil.fromBukkitBlockData(context.getLocation().getBlock().getBlockData())));
        }, context.getLocation());
    }
}

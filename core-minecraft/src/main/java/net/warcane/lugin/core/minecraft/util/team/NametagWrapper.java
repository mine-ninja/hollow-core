package net.warcane.lugin.core.minecraft.util.team;

import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.warcane.lugin.core.minecraft.util.PacketUtil;
import net.warcane.lugin.core.minecraft.util.ReflectionUtils;
import net.warcane.lugin.core.minecraft.util.ReflectionUtils.Modifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection<String> members, 	    g
 * String prefix, 					c
 * String suffix, 					d
 * String teamName, 				a
 * String paramInt, 				h
 * String packOption, 				i
 * String displayName, 			    b
 * String push, 					NA
 * String visibility				e
 * <p>
 * 1.8 			("g", "c", "d", "a", "h", "i", "b", "NA", "e")
 * 1.9 up to 1.12 	("h", "c", "d", "a", "i", "j", "b", "f", "e")
 */
public class NametagWrapper {

    private final Packet<?> wrappedPacket = new PacketPlayOutScoreboardTeam();

    public NametagWrapper(String name, int param, List<String> members) {
        if (param != 3 && param != 4) {
            throw new IllegalArgumentException("Method must be join or leave for player constructor");
        }

        setupDefaults(name, param);
        setupMembers(members);
    }

    public NametagWrapper(String name, String prefix, String suffix, int param, Collection<?> players) {
        setupDefaults(name, param);

        if (param == 0 || param == 2) {
            ReflectionUtils.modifyClass(wrappedPacket,
              new Modifier<String>("b", name),
              new Modifier<String>("c", prefix),
              new Modifier<String>("d", suffix),
              new Modifier<Integer>("i", 1));

            if (param == 0) {
                ReflectionUtils.modifyClass(wrappedPacket, new Modifier<Collection<?>>("g", players));
            }
        }
    }

    private void setupMembers(Collection<?> players) {
        players = (players == null || players.isEmpty()) ? new ArrayList<>() : players;
        ReflectionUtils.modifyClass(wrappedPacket, new Modifier<>("g", players));
    }

    private void setupDefaults(String name, int param) {
        net.warcane.lugin.core.minecraft.util.ReflectionUtils.modifyClass(wrappedPacket, new Modifier<String>("a", name), new Modifier<Integer>("h", param));
    }

    public void send() {
        PacketUtil.broadcastPacket(wrappedPacket);
    }

    public void send(Player player) {
        PacketUtil.sendPacket(player, wrappedPacket);
    }

}

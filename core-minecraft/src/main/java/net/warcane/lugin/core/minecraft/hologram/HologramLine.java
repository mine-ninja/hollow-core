package net.warcane.lugin.core.minecraft.hologram;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.warcane.lugin.core.minecraft.util.EntityIdUtil;
import net.warcane.lugin.core.minecraft.util.PacketUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Setter
@Getter
public class HologramLine {

    /**
     * Flags para Armor Stand:
     * - 0x01: Small (pequeno)
     * - 0x04: No gravity (sem gravidade)
     * - 0x08: Marker (não colide)
     */
    private static final byte ARMOR_STAND_FLAGS = (byte) (0x01 | 0x04 | 0x08);

    private final Hologram parent;
    private final int entityId;
    private Location location;

    // Function to get the line text for a specific player
    private Function<Player, String> line;


    HologramLine(@NotNull Hologram parent, @NotNull Location location, @NotNull Function<Player, String> line) {
        this.parent = parent;
        this.entityId = EntityIdUtil.nextEntityId();
        this.location = location;
        this.line = line;
    }

    public void teleport(@NotNull Player player) {
        this.location = player.getLocation();
        PacketUtil.sendPacket(player, PacketUtil.buildTeleportPacket(entityId, location));
    }

    public void showTo(@NotNull Player player) {

        try {
            Packet<?> spawnPacket = createSpawnPacket(player);
            PacketUtil.sendPacket(player, spawnPacket);

//            String text = line.apply(player);
//            System.out.println(
//              "Showing hologram to " + player.getName() +
//              " | EntityID: " + entityId +
//              " | Text: '" + text + "'" +
//              " | Location: " + location.toString()
//            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to show hologram line to player: " + player.getName(), e);
        }
    }

    public void hideTo(@NotNull Player player) {
        PacketUtil.sendPacket(player, PacketUtil.buildDestroyPacket(entityId));
    }

    public void updateLine(@NotNull Player player) {
        try {
            Packet<?> updatePacket = createUpdateMetadataPacket(player);
            PacketUtil.sendPacket(player, updatePacket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Packet<?> createSpawnPacket(@NotNull Player viewer) {
        DataWatcher dataWatcher = createDataWatcher(viewer);
        return PacketUtil.buildSpawnerEntityLivingPacket(dataWatcher, entityId, 30, location);
    }

    public Packet<?> createUpdateMetadataPacket(@NotNull Player viewer) {
        DataWatcher dataWatcher = new DataWatcher(null);
        dataWatcher.a(2, line.apply(viewer));
        return new PacketPlayOutEntityMetadata(entityId, dataWatcher, true);
    }

    private DataWatcher createDataWatcher(@NotNull Player viewer) {
        String customName = this.line.apply(viewer);
        DataWatcher watcher = new DataWatcher(null);
        watcher.a(0, (byte) 0x20);
        watcher.a(1, (short) 300);
        watcher.a(2, customName);
        watcher.a(3, (byte) 1);
        watcher.a(4, (byte) 1); // No item in hand
        watcher.a(5, (byte) 1);
        watcher.a(10, (byte) 0x16);

        return watcher;
    }
}
package net.warcane.lugin.core.minecraft.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static net.warcane.lugin.core.minecraft.util.UtilReflection.setFieldValue;

public class PacketUtil {

    public static void sendPackets(@NotNull Player player, @NotNull Packet<?>... packets) {
        for (Packet<?> packet : packets) {
            sendPacketToPlayer(player, packet);
        }
    }


    public static void sendPacketToPlayer(@NotNull Player player, @NotNull Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public static void broadcastPacket(@NotNull Packet<?> packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPacketToPlayer(player, packet);
        }
    }

    public static void broadcastPacket(@NotNull Packet<?> packet, @NotNull World world) {
        for (Player player : world.getPlayers()) {
            sendPacketToPlayer(player, packet);
        }
    }

    public static void broadcastPacket(@NotNull Packet<?> packet, @NotNull Player... players) {
        for (Player player : players) {
            sendPacketToPlayer(player, packet);
        }
    }

    public static void broadcastPacket(@NotNull Packet<?> packet, @NotNull Predicate<Player> filter) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                sendPacketToPlayer(player, packet);
            }
        }
    }


    public static PacketPlayOutSpawnEntityLiving buildSpawnerEntityLivingPacket(DataWatcher watcher, int entityId, int typeId, Location location) {
        PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving();
        setFieldValue(packet, "a", entityId);
        setFieldValue(packet, "b", typeId); // serverId da entidade
        setFieldValue(packet, "c", floor(location.getX() * 32D));
        setFieldValue(packet, "d", floor(location.getY() * 32D));
        setFieldValue(packet, "e", floor(location.getZ() * 32D));
        setFieldValue(packet, "l", watcher);
        return packet;
    }

    public static PacketPlayOutSpawnEntity buildSpawnerEntityPacket(int entityId, Location location) {
        PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity();
        setFieldValue(packet, "a", entityId);
        setFieldValue(packet, "b", floor(location.getX() * 32D));
        setFieldValue(packet, "c", floor(location.getY() * 32D));
        setFieldValue(packet, "d", floor(location.getZ() * 32D));

        setFieldValue(packet, "h", floor(location.getPitch() * 256.0F / 360.0F));
        setFieldValue(packet, "i", floor(location.getYaw() * 256.0F / 360.0F));

        setFieldValue(packet, "j", 2);
        setFieldValue(packet, "k", 100);

        return packet;
    }

    public static PacketPlayOutNamedEntitySpawn buildSpawnHumanPacket(int entityId, Location loc, GameProfile profile) {
        DataWatcher dataWatcher = new DataWatcher(null);
        dataWatcher.a(10, (byte) 127); // (0b01111111) ativando todas as partes da skin para 1.8+
        dataWatcher.a(6, (float) 20);

        PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();
        setFieldValue(packet, "a", entityId);//serverId
        setFieldValue(packet, "b", profile.getId());//uuid
        setFieldValue(packet, "c", floor(loc.getX() * 32));//x
        setFieldValue(packet, "d", floor(loc.getY() * 32));//y
        setFieldValue(packet, "e", floor(loc.getZ() * 32));//z
        setFieldValue(packet, "f", getCompressedAngle(loc.getYaw()));//yaw
        setFieldValue(packet, "g", getCompressedAngle(loc.getPitch()));//pitch
        setFieldValue(packet, "h", 0); // item hand
        setFieldValue(packet, "i", dataWatcher);

        return packet;
    }

    public static PacketPlayOutEntityDestroy buildDestroyPacket(int... entityIds) {
        return new PacketPlayOutEntityDestroy(entityIds);
    }

    public static PacketPlayOutEntityTeleport buildTeleportPacket(int entityId, Location location) {
        return new PacketPlayOutEntityTeleport(entityId,
          MathHelper.floor(location.getX() * 32.0D),
          MathHelper.floor(location.getY() * 32.0D),
          MathHelper.floor(location.getZ() * 32.0D),
          (byte) ((int) (location.getYaw() * 256.0F / 360.0F)),
          (byte) ((int) (location.getPitch() * 256.0F / 360.0F)),
          false
        );
    }

    public static PacketPlayOutEntityHeadRotation buildEntityHeadRotationPacket(int entityId, float yaw) {
        PacketPlayOutEntityHeadRotation packet = new PacketPlayOutEntityHeadRotation();
        setFieldValue(packet, "a", entityId);
        setFieldValue(packet, "b", getCompressedAngle(yaw));
        return packet;
    }

    public static PacketPlayOutEntityHeadRotation buildEntityHeadRotationPacket(Entity entity, float yaw) {
        return new PacketPlayOutEntityHeadRotation(entity, getCompressedAngle(yaw));
    }

    public static PacketPlayOutAttachEntity buildAttachPacket(int entityA, int entityB) {
        PacketPlayOutAttachEntity packet = new PacketPlayOutAttachEntity();
        setFieldValue(packet, "a", 0);
        setFieldValue(packet, "b", entityA);
        setFieldValue(packet, "c", entityB);
        return packet;
    }

    public static PacketPlayOutEntityEquipment buildEquipmentPacket(int entityId, int slot, ItemStack item) {
        if (item == null) {
            item = new ItemStack(Material.AIR);
        }

        return new PacketPlayOutEntityEquipment(entityId, slot, CraftItemStack.asNMSCopy(item));
    }

    public static PacketPlayOutAnimation buildPunchAnimationPacket(int entityId) {
        PacketPlayOutAnimation packet = new PacketPlayOutAnimation();

        setFieldValue(packet, "a", entityId);
        setFieldValue(packet, "b", 0);

        return packet;
    }

    public static int floor(double var0) {
        int var2 = (int) var0;
        return var0 < (double) var2 ? var2 - 1 : var2;
    }

    public static byte getCompressedAngle(float value) {
        return (byte) ((int) value * 256.0F / 360.0F);
    }

    public static DataWatcher clonePlayerDataWatcher(Player player, int currentEntId) {
        EntityHuman h = new EntityHuman(((CraftWorld) player.getWorld()).getHandle(), ((CraftPlayer) player).getProfile()) {
            public void sendMessage(IChatBaseComponent arg0) {
            }

            public boolean a(int arg0, String arg1) {
                return false;
            }

            public BlockPosition getChunkCoordinates() {
                return null;
            }

            public boolean isSpectator() {
                return false;
            }
        };
        h.d(currentEntId);
        return h.getDataWatcher();
    }


    public static void sendPacket(@NotNull Player player, @NotNull Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}

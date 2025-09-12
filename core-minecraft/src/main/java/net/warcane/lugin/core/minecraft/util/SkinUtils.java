package net.warcane.lugin.core.minecraft.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.extern.log4j.Log4j2;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

@Log4j2
public class SkinUtils {

    private static final String NMS = Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "net.minecraft.server");
    private static final String CB = Bukkit.getServer().getClass().getPackage().getName();

    private static Class<?> getNmsClass(String name) throws Exception {
        return Class.forName(NMS + "." + name);
    }

    private static Class<?> getCbClass() throws Exception {
        return Class.forName(CB + "." + "entity.CraftPlayer");
    }

    private static void sendPacket(Player receiver, Object packet) throws Exception {
        Object craftPlayer = getCbClass().cast(receiver);
        Object handle = invokeExact(craftPlayer, "getHandle");
        Object connection = FieldUtils.readField(handle, "playerConnection", true);

        invoke(connection, "sendPacket", packet);
    }

    public static void setPlayerSkin(Player player, String texture, String signature) {
        Bukkit.getScheduler().runTask(BukkitPlatform.getInstance().getPlugin(), () -> {
            try {
                Object craftPlayer = getCbClass().cast(player);
                Object entityPlayer = invokeExact(craftPlayer, "getHandle");
                Object entity = getNmsClass("Entity").cast(entityPlayer);

                GameProfile profile = (GameProfile) invokeExact(craftPlayer, "getProfile");

                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", texture, signature));

                Class<?> packetInfoClass = getNmsClass("PacketPlayOutPlayerInfo");
                Class<?> enumPlayerInfoAction = getNmsClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

                Constructor<?> packetInfoCtor = packetInfoClass.getConstructor(enumPlayerInfoAction, Iterable.class);

                Object removeInfo = packetInfoCtor.newInstance(
                    enumPlayerInfoAction.getEnumConstants()[4],
                    Collections.singletonList(entityPlayer)
                );

                Object addInfo = packetInfoCtor.newInstance(
                    enumPlayerInfoAction.getEnumConstants()[0],
                    Collections.singletonList(entityPlayer)
                );

                Constructor<?> destroyCtor = getNmsClass("PacketPlayOutEntityDestroy").getConstructor(int[].class);
                Object destroy = destroyCtor.newInstance((Object) new int[]{(int) invokeExact(entity, "getId")});

                Constructor<?> respawnCtor = getNmsClass("PacketPlayOutRespawn")
                    .getConstructor(int.class,
                        getNmsClass("EnumDifficulty"),
                        getNmsClass("WorldType"),
                        getNmsClass("WorldSettings$EnumGamemode"));

                Object world = invokeExact(entityPlayer, "getWorld");

                Object respawn = respawnCtor.newInstance(
                    0,
                    invokeExact(world, "getDifficulty"),
                    invokeExact(FieldUtils.readField(world, "worldData", true), "getType"),
                    FieldUtils.readField(entityPlayer, "playerInteractManager", true).getClass().getMethod("getGameMode").invoke(FieldUtils.readField(entityPlayer, "playerInteractManager", true))
                );

                Location loc = player.getLocation();

                Object spawn = getNmsClass("PacketPlayOutNamedEntitySpawn").getConstructor(getNmsClass("EntityHuman")).newInstance(entityPlayer);
                Object pos = getNmsClass("PacketPlayOutPosition").getConstructor(double.class, double.class, double.class, float.class, float.class, java.util.Set.class).newInstance(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), new HashSet<>());
                Object slot = getNmsClass("PacketPlayOutHeldItemSlot").getConstructor(int.class).newInstance(player.getInventory().getHeldItemSlot());

                for (Player target : player.getWorld().getPlayers()) {
                    if (target.canSee(player)) {
                        sendPacket(target, removeInfo);

                        if (target != player)
                            sendPacket(target, destroy);
                    }
                }

                sendPacket(player, respawn);

                for (Player target : player.getWorld().getPlayers()) {
                    if (target.canSee(player)) {
                        sendPacket(target, addInfo);

                        if (target != player)
                            sendPacket(target, spawn);
                    }
                }

                sendPacket(player, removeInfo);
                sendPacket(player, destroy);
                sendPacket(player, respawn);
                sendPacket(player, addInfo);
                sendPacket(player, pos);
                sendPacket(player, slot);

                invoke(entityPlayer, "updateAbilities");
                invoke(craftPlayer, "updateScaledHealth");
                invoke(player, "updateInventory");
                invoke(entityPlayer, "triggerHealthUpdate");

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        Bukkit.getScheduler().runTaskLater(BukkitPlatform.getInstance().getPlugin(), () -> {
            if (!PlayerUtil.isOnline(player)) {
                try {
                    Constructor<?> destroyCtor = getNmsClass("PacketPlayOutEntityDestroy").getConstructor(int[].class);
                    Object destroy = destroyCtor.newInstance((Object) new int[]{player.getEntityId()});

                    for (Player target : Bukkit.getOnlinePlayers())
                        sendPacket(target, destroy);
                } catch (Exception exception) {
                    log.error(Arrays.toString(exception.getStackTrace()));
                }
            }
        }, 500L);
    }

    private static void invoke(Object target, String methodName, Object... args) throws Exception {
        Class<?> clazz = target.getClass();
        Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

        Method method = findCompatibleMethod(clazz, methodName, argTypes);

        if (method == null)
            throw new NoSuchMethodException(methodName);

        method.setAccessible(true);

        method.invoke(target, args);
    }

    private static Object invokeExact(Object target, String methodName, Object... args) throws Exception {
        Class<?> clazz = target.getClass();
        Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

        Method method = clazz.getMethod(methodName, argTypes);
        method.setAccessible(true);

        return method.invoke(target, args);
    }

    private static Method findCompatibleMethod(Class<?> clazz, String name, Class<?>[] argTypes) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name))
                continue;

            Class<?>[] params = m.getParameterTypes();

            if (params.length != argTypes.length)
                continue;

            boolean compatible = true;

            for (int i = 0; i < params.length; i++) {
                if (!params[i].isAssignableFrom(argTypes[i])) {
                    compatible = false;
                    break;
                }
            }

            if (compatible)
                return m;
        }

        return null;
    }
}

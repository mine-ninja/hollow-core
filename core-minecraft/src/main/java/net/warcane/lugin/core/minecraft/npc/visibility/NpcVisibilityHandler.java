package net.warcane.lugin.core.minecraft.npc.visibility;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.PlayerInfoData;
import net.minecraft.server.v1_8_R3.ScoreboardTeamBase.EnumNameTagVisibility;
import net.warcane.lugin.core.minecraft.npc.Npc;
import net.warcane.lugin.core.minecraft.npc.provider.NpcSkinProvider;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.GameProfileUtil;
import net.warcane.lugin.core.minecraft.util.PacketUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.warcane.lugin.core.minecraft.util.PacketUtil.buildSpawnHumanPacket;
import static net.warcane.lugin.core.minecraft.util.UtilReflection.getFieldValue;
import static net.warcane.lugin.core.minecraft.util.UtilReflection.setFieldValue;

@Slf4j
@Getter
@Setter
public class NpcVisibilityHandler {

    private static double DEFAULT_RANGE = NumberConversions.square(Bukkit.getViewDistance() << 4);
    private static final Scoreboard SCOREBOARD = new Scoreboard();

    private final Npc npc;
    private final GameProfile profile;
    private final ScoreboardTeam scoreboardTeam;

    private Predicate<Player> renderFilter = player -> true;
    private NpcSkinProvider skinProvider;

    private final Set<UUID> viewers = new HashSet<>();


    public NpcVisibilityHandler(@NotNull Npc npc, @Nullable NpcSkinProvider npcSkinProvider) {
        this.npc = npc;
        this.skinProvider = npcSkinProvider;

        var randomName = RandomStringUtils.randomAlphabetic(6);
        this.scoreboardTeam = SCOREBOARD.createTeam(randomName);
        this.scoreboardTeam.setNameTagVisibility(EnumNameTagVisibility.NEVER);

        this.profile = GameProfileUtil.createGameProfile("§0[NPC] " + randomName);
        this.scoreboardTeam.getPlayerNameSet().add(this.profile.getName());
    }

    public NpcVisibilityHandler(Npc npc) {
        this(npc, null);
    }

    public boolean isShown(@NotNull Player player) {
        return this.viewers.contains(player.getUniqueId());
    }

    public boolean canView(@NotNull Player player) {
        return player.isOnline()
               && renderFilter.test(player)
               && isInRange(player);
    }

    public boolean isInRange(@NotNull Player player) {
        var playerLocation = player.getLocation();
        var npcLocation = this.npc.getLocation();
        return npcLocation.getWorld().equals(playerLocation.getWorld())
               && playerLocation.distanceSquared(npcLocation) <= DEFAULT_RANGE;
    }

    public void refreshTo(Player player) {
        this.hideTo(player);
        this.showTo(player);
    }

    public void showTo(@NotNull Player player) {
        if (viewers.contains(player.getUniqueId()) || !canView(player)) return;

        if (skinProvider != null) {
            var skin = skinProvider.provideSkin(npc, player);
            if (skin != null) {
                log.info("Render skin listener returned skin for player: {}, NPC: {}", player.getName(), npc.getEntityId());
                profile.getProperties().clear();
                profile.getProperties().put("textures", skin.getProperty());
            } else {
                log.warn("Render skin listener returned null for player: {}, NPC: {}", player.getName(), npc.getEntityId());
            }
        }

        var playerInfoPacket = buildPlayerInfoPacket(EnumPlayerInfoAction.ADD_PLAYER, profile);
        var spawnPacket = buildSpawnHumanPacket(npc.getEntityId(), npc.getLocation(), profile);
        var headRotationPacket = buildEntityHeadRotationPacket();
        var firstTeamPacket = buildPacketPlayOutScoreboardTeam(1);
        var secondTeamPacket = buildPacketPlayOutScoreboardTeam(0);

        PacketUtil.sendPackets(
          player,
          playerInfoPacket,
          spawnPacket,
          headRotationPacket,
          firstTeamPacket,
          secondTeamPacket
        );


        viewers.add(player.getUniqueId());

        Tasks.runAsyncLater(() -> {
            var updatePacket = buildPlayerInfoPacket(EnumPlayerInfoAction.REMOVE_PLAYER, profile);
            PacketUtil.sendPacket(player, updatePacket);
        }, 50L);
    }

    public void showToAll() {
        npc.getWorld()
          .getPlayers()
          .stream()
          .filter(this::canView)
          .forEach(this::showTo);
    }

    public void hideTo(@NotNull Player player) {
        if (!viewers.contains(player.getUniqueId())) return;

        var destroyPacket = PacketUtil.buildDestroyPacket(npc.getEntityId());
        PacketUtil.sendPacket(player, destroyPacket);

        viewers.remove(player.getUniqueId());
    }


    public void hideToAll() {
        npc.getWorld()
          .getPlayers()
          .forEach(this::hideTo);
    }

    public void forEachViewers(@NotNull Consumer<Player> action) {
        for (UUID viewerId : viewers) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    private static PacketPlayOutPlayerInfo buildPlayerInfoPacket(EnumPlayerInfoAction action, GameProfile profile) {
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
        setFieldValue(packet, "a", action);

        List<PlayerInfoData> dataList = getFieldValue(packet, "b");
        IChatBaseComponent nameComponent = ChatSerializer.a("{\"text\":\"" + profile.getName() + "\"}");
        dataList.add(packet.new PlayerInfoData(profile, 1, WorldSettings.EnumGamemode.NOT_SET, nameComponent));

        return packet;
    }

    protected PacketPlayOutEntityHeadRotation buildEntityHeadRotationPacket() {
        return PacketUtil.buildEntityHeadRotationPacket(npc.getEntityId(), npc.getLocation().getYaw());
    }

    public Packet<?> buildPacketPlayOutScoreboardTeam(int value) {
        return new PacketPlayOutScoreboardTeam(scoreboardTeam, value);
    }

}

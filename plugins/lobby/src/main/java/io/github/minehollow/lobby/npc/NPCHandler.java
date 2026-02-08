package io.github.minehollow.lobby.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.github.minehollow.lobby.hologram.HologramHandler;
import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import me.tofaa.entitylib.wrapper.WrapperPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Getter
public final class NPCHandler {
    private static final double HOLOGRAM_Y_OFFSET = 2.1;
    private static final byte ALL_SKIN_LAYERS;

    static {
        byte mask = 0;
        // if (visibleSkinLayers.contains(SkinLayer.CAPE)) mask |= 0x01;
        //        if (visibleSkinLayers.contains(SkinLayer.JACKET)) mask |= 0x02;
        //        if (visibleSkinLayers.contains(SkinLayer.LEFT_SLEEVE)) mask |= 0x04;
        //        if (visibleSkinLayers.contains(SkinLayer.RIGHT_SLEEVE)) mask |= 0x08;
        //        if (visibleSkinLayers.contains(SkinLayer.LEFT_PANTS)) mask |= 0x10;
        //        if (visibleSkinLayers.contains(SkinLayer.RIGHT_PANTS)) mask |= 0x20;
        //        if (visibleSkinLayers.contains(SkinLayer.HAT)) mask |= 0x40;

        mask |= 0x01; // Cape
        mask |= 0x02; // Jacket
        mask |= 0x04; // Left Sleeve
        mask |= 0x08; // Right Sleeve
        mask |= 0x10; // Left Pants
        mask |= 0x20; // Right Pants
        mask |= 0x40; // Hat

        ALL_SKIN_LAYERS = mask;
    }

    private WrapperPlayer npcEntity;
    private NPCData data;
    private final HologramManager hologramManager;

    public NPCHandler(@NotNull NPCData data, @NotNull HologramManager hologramManager) {
        this.data = data;
        this.hologramManager = hologramManager;
        setupEntity();
    }

    private void setupEntity() {
        // Criar perfil com UUID aleatório para evitar conflitos de skins de players reais
        UserProfile userProfile = new UserProfile(UUID.randomUUID(), data.name());

        // Aplicar texturas se existirem
        if (data.skinTexture() != null && data.skinSignature() != null) {
            userProfile.setTextureProperties(List.of(
              new TextureProperty("textures", data.skinTexture(), data.skinSignature())
            ));
        }

        // Registrar ID na EntityLib
        var entityId = EntityLib.getPlatform().getEntityIdProvider()
          .provide(UUID.randomUUID(), EntityTypes.PLAYER);

        npcEntity = new WrapperPlayer(userProfile, entityId);
        npcEntity.setInTablist(false);
        npcEntity.consumeEntityMeta(PlayerMeta.class, meta -> {
            meta.setCustomName(Component.empty());
            meta.setCustomNameVisible(false);
        });

        // Spawnar na localização convertida
        var location = SpigotConversionUtil.fromBukkitLocation(data.location());
        npcEntity.spawn(location);
    }


    /**
     * Envia o pacote de metadados para ativar as camadas 3D.
     * O índice 17 é o padrão para 'Skin Customization' em versões modernas.
     * if (wrapperPlayer == null) {
     * return;
     * }
     * <p>
     * byte skinMask = playerAppearance.getSkinLayerMask();
     * // Index 16 = Displayed Skin Parts (Byte) per MC protocol wiki
     * EntityData skinData = new EntityData(16, EntityDataTypes.BYTE, skinMask);
     * WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
     * entityId, List.of(skinData)
     * );
     * <p>
     * for (Viewer viewer : viewers) {
     * if (viewer instanceof PaperViewer paperViewer) {
     * PacketEvents.getAPI().getPlayerManager()
     * .sendPacket(paperViewer.getPlayer(), packet);
     * }
     * }
     */
    @SuppressWarnings("all")
    public void applyMetadata(@NotNull Player player) {
        // 1. Esconder o Nome usando um Time do Scoreboard (via Packet)
        WrapperPlayServerTeams teamPacket = generateTeam();

        // Index 17 (Byte) = Skin Layers mask.

        final var skinData = new EntityData(16, EntityDataTypes.BYTE, ALL_SKIN_LAYERS);
        final var packet = new WrapperPlayServerEntityMetadata(npcEntity.getEntityId(), List.of(skinData));
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();

        playerManager.sendPacket(player, packet);
        playerManager.sendPacket(player, teamPacket);
    }

    @NotNull
    private WrapperPlayServerTeams generateTeam() {
        final var teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
          Component.empty(), // Display Name
          Component.empty(), // Prefix
          Component.empty(), // Suffix
          WrapperPlayServerTeams.NameTagVisibility.NEVER,
          WrapperPlayServerTeams.CollisionRule.NEVER,
          NamedTextColor.WHITE,
          WrapperPlayServerTeams.OptionData.NONE
        );

        // Criar o time e adicionar o NPC a ele
        WrapperPlayServerTeams teamPacket = new WrapperPlayServerTeams(
          "npc_team_" + npcEntity.getEntityId(),
          WrapperPlayServerTeams.TeamMode.CREATE,
          teamInfo,
          data.name() // Adiciona o nome do NPC ao time
        );
        return teamPacket;
    }

    public void spawn() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            addViewer(player);
        }
    }

    public void remove() {
        npcEntity.despawn();
        npcEntity.remove();
    }

    public void teleport(@NotNull Location location) {
        this.data = new NPCData(data.name(), location, data.skinTexture(),
          data.skinSignature(), data.hologramId(), data.interaction());

        npcEntity.teleport(SpigotConversionUtil.fromBukkitLocation(location));

        if (data.hologramId() != null) {
            Location hologramLocation = location.clone().add(0, HOLOGRAM_Y_OFFSET, 0);
            hologramManager.teleportHologram(data.hologramId(), hologramLocation);
        }
    }

    public void updateSkin(@NotNull String texture, @NotNull String signature) {
        this.data = new NPCData(data.name(), data.location(), texture, signature,
          data.hologramId(), data.interaction());

        // Salva os viewers atuais antes de recriar
        var currentViewers = List.copyOf(npcEntity.getViewers());

        npcEntity.remove();
        setupEntity();

        // Readiciona os viewers e envia o pacote de camadas
        for (UUID uuid : currentViewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                addViewer(player);
            }
        }
    }

    public void addViewer(@NotNull Player player) {
        npcEntity.addViewer(player.getUniqueId());


        // Delay de 10 ticks (0.5s) para garantir que a entidade foi carregada no cliente
        // antes de enviarmos o metadado da skin.
        Tasks.runAsyncLater(() -> {
            if (player.isOnline() && npcEntity.getViewers().contains(player.getUniqueId())) {
                applyMetadata(player);
            }
        }, 10L);
    }

    public void removeViewer(@NotNull Player player) {
        npcEntity.removeViewer(player.getUniqueId());
    }

    public void setHologram(@Nullable String hologramId) {
        this.data = new NPCData(data.name(), data.location(), data.skinTexture(),
          data.skinSignature(), hologramId, data.interaction());
    }

    public void updateInteraction(@Nullable String interaction) {
        this.data = new NPCData(data.name(), data.location(), data.skinTexture(),
          data.skinSignature(), data.hologramId(), interaction);
    }

    @Nullable
    public HologramHandler getAttachedHologram() {
        if (data.hologramId() == null) return null;
        return hologramManager.getHologram(data.hologramId());
    }
}
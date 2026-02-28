package io.github.minehollow.npc.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import io.github.minehollow.npc.api.NpcClickType;
import io.github.minehollow.npc.config.NpcConfig;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Core NPC implementation backed by EntityLib's {@link WrapperPlayer}.
 * The NPC is a fake player entity — no real Bukkit entity is created.
 * Hologram lines use {@link WrapperEntity} with {@code TEXT_DISPLAY} type.
 */
public class NpcImpl implements Npc {

    private static final byte ALL_SKIN_LAYERS = (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final NpcConfig config;
    private final double renderDistance;

    // Entities
    private WrapperPlayer playerEntity;
    private final List<WrapperEntity> hologramEntities = new ArrayList<>();

    // Viewers tracking
    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    private boolean spawned = false;

    public NpcImpl(@NotNull NpcConfig config, double renderDistance) {
        this.config = config;
        this.renderDistance = renderDistance;
    }

    // ── Npc interface ────────────────────────────────────────

    @Override
    public @NotNull String getId() {
        return config.getId();
    }

    @Override
    public @NotNull Location getLocation() {
        return config.getLocation();
    }

    @Override
    public void teleport(@NotNull Location location) {
        config.setLocation(location);
        if (playerEntity != null) {
            playerEntity.teleport(SpigotConversionUtil.fromBukkitLocation(location));
        }
        rebuildHolograms();
    }

    @Override
    public void setSkin(@NotNull String value, @NotNull String signature) {
        config.setSkin(value, signature);
        if (!spawned) return;

        // Must re-create the entity to apply new skin
        Set<UUID> oldViewers = new CopyOnWriteArraySet<>(viewers);
        despawn();
        spawn();
        for (UUID uuid : oldViewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                addViewer(player);
            }
        }
    }

    @Override
    public @Nullable String getSkinValue() {
        return config.getSkinValue();
    }

    @Override
    public @Nullable String getSkinSignature() {
        return config.getSkinSignature();
    }

    @Override
    public void setScale(double scale) {
        config.setScale(scale);
        if (playerEntity != null) {
            // Scale attribute — entity data index 16+ depends on version
            // For 1.20.5+ the scale is attribute-based, applied via EntityLib meta
            playerEntity.consumeEntityMeta(PlayerMeta.class, meta -> {
                // PlayerMeta doesn't expose scale directly;
                // we send raw metadata for the generic.scale attribute
            });
            // Send scale via raw packet
            sendScaleMetadata();
        }
        rebuildHolograms();
    }

    @Override
    public double getScale() {
        return config.getScale();
    }

    @Override
    public @NotNull List<String> getHologramLines() {
        return config.getHologramLines();
    }

    @Override
    public void setHologramLine(int index, @NotNull String text) {
        if (index >= 0 && index < config.getHologramLines().size()) {
            config.getHologramLines().set(index, text);
            rebuildHolograms();
        }
    }

    @Override
    public void addHologramLine(@NotNull String text) {
        config.getHologramLines().add(text);
        rebuildHolograms();
    }

    @Override
    public void removeHologramLine(int index) {
        if (index >= 0 && index < config.getHologramLines().size()) {
            config.getHologramLines().remove(index);
            rebuildHolograms();
        }
    }

    @Override
    public double getHologramOffset() {
        return config.getHologramOffset();
    }

    @Override
    public void setHologramOffset(double offset) {
        config.setHologramOffset(offset);
        rebuildHolograms();
    }

    @Override
    public @NotNull List<NpcAction> getActions() {
        return config.getActions();
    }

    @Override
    public void addAction(@NotNull NpcAction action) {
        config.getActions().add(action);
    }

    @Override
    public void clearActions() {
        config.getActions().clear();
    }

    @Override
    public @NotNull NpcClickType getClickType() {
        return config.getClickType();
    }

    @Override
    public void setClickType(@NotNull NpcClickType type) {
        config.setClickType(type);
    }

    @Override
    public void spawn() {
        if (spawned) return;
        createPlayerEntity();
        createHologramEntities();
        spawned = true;
    }

    @Override
    public void despawn() {
        if (!spawned) return;
        spawned = false;
        viewers.clear();

        if (playerEntity != null) {
            playerEntity.despawn();
            playerEntity.remove();
            playerEntity = null;
        }
        for (WrapperEntity holo : hologramEntities) {
            holo.despawn();
            holo.remove();
        }
        hologramEntities.clear();
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public int getEntityId() {
        return playerEntity != null ? playerEntity.getEntityId() : -1;
    }

    // ── Viewer management ────────────────────────────────────

    public void addViewer(@NotNull Player player) {
        if (!spawned || playerEntity == null) return;
        UUID uuid = player.getUniqueId();
        if (!viewers.add(uuid)) return;

        playerEntity.addViewer(uuid);
        for (WrapperEntity holo : hologramEntities) {
            holo.addViewer(uuid);
        }

        // Delay metadata so the client has time to process the spawn
        Tasks.runAsyncLater(() -> {
            if (player.isOnline() && viewers.contains(uuid)) {
                sendSkinMetadata(player);
                sendTeamPacket(player);
            }
        }, 10L);
    }

    public void removeViewer(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (!viewers.remove(uuid)) return;

        if (playerEntity != null) {
            playerEntity.removeViewer(uuid);
        }
        for (WrapperEntity holo : hologramEntities) {
            holo.removeViewer(uuid);
        }
    }

    public boolean isViewer(@NotNull UUID uuid) {
        return viewers.contains(uuid);
    }

    public double getRenderDistanceSq() {
        return renderDistance * renderDistance;
    }

    public @NotNull NpcConfig getConfig() {
        return config;
    }

    // ── Entity creation ──────────────────────────────────────

    private void createPlayerEntity() {
        UserProfile profile = new UserProfile(UUID.randomUUID(), config.getId());

        if (config.getSkinValue() != null && config.getSkinSignature() != null) {
            profile.setTextureProperties(List.of(
                new TextureProperty("textures", config.getSkinValue(), config.getSkinSignature())
            ));
        }

        int entityId = EntityLib.getPlatform().getEntityIdProvider()
            .provide(UUID.randomUUID(), EntityTypes.PLAYER);

        playerEntity = new WrapperPlayer(profile, entityId);
        playerEntity.setInTablist(false);
        playerEntity.consumeEntityMeta(PlayerMeta.class, meta -> {
            meta.setCustomName(Component.empty());
            meta.setCustomNameVisible(false);
        });

        playerEntity.spawn(SpigotConversionUtil.fromBukkitLocation(config.getLocation()));
    }

    private void createHologramEntities() {
        List<String> lines = config.getHologramLines();
        if (lines.isEmpty()) return;

        Location base = config.getLocation().clone().add(0, config.getHologramOffset(), 0);
        double lineSpacing = 0.3 * config.getScale();

        for (int i = 0; i < lines.size(); i++) {
            WrapperEntity textDisplay = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
            TextDisplayMeta meta = (TextDisplayMeta) textDisplay.getEntityMeta();

            Component text = MINI_MESSAGE.deserialize(lines.get(i));
            meta.setText(text);
            meta.setBillboardConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
            meta.setShadow(true);
            meta.setBackgroundColor(0x0);

            // Scale text with NPC scale
            if (config.getScale() != 1.0) {
                float s = (float) config.getScale();
                meta.setScale(new Vector3f(s, s, s));
            }

            Location holoLoc = base.clone().add(0, (lines.size() - 1 - i) * lineSpacing, 0);
            textDisplay.spawn(SpigotConversionUtil.fromBukkitLocation(holoLoc));

            hologramEntities.add(textDisplay);
        }
    }

    private void rebuildHolograms() {
        if (!spawned) return;

        Set<UUID> currentViewers = new CopyOnWriteArraySet<>(viewers);

        // Remove old hologram entities
        for (WrapperEntity holo : hologramEntities) {
            holo.despawn();
            holo.remove();
        }
        hologramEntities.clear();

        // Create new ones
        createHologramEntities();

        // Re-add viewers
        for (UUID uuid : currentViewers) {
            for (WrapperEntity holo : hologramEntities) {
                holo.addViewer(uuid);
            }
        }
    }

    // ── Packet helpers ───────────────────────────────────────

    private void sendSkinMetadata(@NotNull Player player) {
        if (playerEntity == null) return;
        EntityData<Byte> skinData = new EntityData<>(17, EntityDataTypes.BYTE, ALL_SKIN_LAYERS);
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
            playerEntity.getEntityId(), List.of(skinData));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void sendTeamPacket(@NotNull Player player) {
        if (playerEntity == null) return;
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.empty(), Component.empty(), Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.NEVER,
            WrapperPlayServerTeams.CollisionRule.NEVER,
            NamedTextColor.WHITE,
            WrapperPlayServerTeams.OptionData.NONE
        );
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
            "npc_" + playerEntity.getEntityId(),
            WrapperPlayServerTeams.TeamMode.CREATE,
            info,
            config.getId()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void sendScaleMetadata() {
        // In 1.20.5+ scale is entity data index 18 (FLOAT)
        // This may vary by version; for 1.21.4 it's index 18 type FLOAT
        // Note: scale metadata index depends on protocol version.
        // We'll skip raw scale metadata for now — the hologram scaling handles visual feedback.
    }
}

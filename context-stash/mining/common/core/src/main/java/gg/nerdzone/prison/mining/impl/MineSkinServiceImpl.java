/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.impl;

import com.fastasyncworldedit.core.extent.clipboard.CPUOptimizedClipboard;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.base.Stopwatch;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import gg.nerdzone.common.location.LocationImmutable;
import gg.nerdzone.common.plugin.BukkitPlugin;
import gg.nerdzone.common.world.BukkitWorldCreator;
import gg.nerdzone.common.world.WorldParams;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.skin.Skin;
import gg.nerdzone.prison.mining.model.skin.enums.SkinType;
import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.model.theme.ThemeStats;
import gg.nerdzone.prison.mining.position.MiningPosition;
import gg.nerdzone.prison.scheduler.CoreSchedulerHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Skin service is responsible for skin world handling
 *
 * @see Skin
 * @see SkinType
 * @see MineTheme
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MineSkinServiceImpl {

    public static @NotNull MineSkinServiceImpl create() {
        return new MineSkinServiceImpl();
    }

    private static final String SCHEMATIC_EXTENSION = "schem3";

    private static final Map<String, Skin> SKINS = new ConcurrentHashMap<>();

    private static final Collection<String> WORLDS = ConcurrentHashMap.newKeySet();

    public void init(@NonNull BukkitPlugin plugin) {
        CoreSchedulerHandler.INSTANCE.runLater(
            "mines-world-init",
            () -> {
                for (final MineTheme theme : MineTheme.values()) {
                    this.loadSkin(theme, plugin);
                }
            }, 2L
        );
    }

    public void loadSkin(@NonNull MineTheme theme, @NonNull BukkitPlugin plugin) {
        final SkinType skinType = theme.getStats().skin();
        final World world = this.loadWorld(skinType.getId(), theme, plugin);
        final Skin skin = new Skin(skinType);
        SKINS.put(skinType.getId(), skin);

        TaskManager.taskManager().async(() -> {
            final String buildFileName = skinType.getBuildFile();
            final ClassLoader classLoader = this.getClass().getClassLoader();

            try (final InputStream buildStream = classLoader.getResourceAsStream("skins/" + buildFileName)) {
                if (Objects.isNull(buildStream)) {
                    plugin.getLogger().info("Could not find skin: " + buildFileName + ". Skipping it");
                    return;
                }

                this.paste(skin, buildFileName, world, buildStream, plugin);
            } catch (IOException exception) {
                throw new RuntimeException("Could not load skin: %s".formatted(buildFileName), exception);
            }
        });
    }

    public boolean isSkinWorld(@NotNull World world) {
        return WORLDS.contains(world.getName());
    }

    public @Nullable Skin findSkin(MineTheme theme) {
        return SKINS.get(theme.getSkin());
    }

    public @NotNull World findWorld(@NonNull MineTheme theme) {
        final Skin skin = Objects.requireNonNull(SKINS.get(theme.getSkin().toLowerCase()), "Skin not found for theme: " + theme.name());
        return BukkitWorldCreator.getByName(skin.getId());
    }

    public @NotNull MineBlockPosition getMiningAreaCenter(@NotNull MineTheme theme) {
        final ThemeStats stats = theme.getStats();
        final SkinType skinType = stats.skin();
        final MineBlockPosition spawn = stats.spawn().toBlockPos();
        final MineBlockPosition areaOffset = skinType.getMineAreaOffset().toBlockPos();
        return spawn.offset(areaOffset);
    }

    public @NotNull String getFormattedWorldName(@NotNull MineTheme theme) { // Real world name (Used usually in teleport contexts)
        return BukkitWorldCreator.PREFIX + SkinType.valueOf(theme.getSkin()).getId();
    }

    @Internal
    private @NotNull World loadWorld(String name, MineTheme theme, BukkitPlugin plugin) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final ThemeStats stats = theme.getStats();
        final MiningPosition spawn = stats.spawn();

        final MineBlockPosition center = this.getMiningAreaCenter(theme); // Border center

        final WorldParams params = WorldParams.builder()
            .name(name)
            .borderRadius(stats.borderRadius())
            .centerX(center.x())
            .centerZ(center.z())
            .spawnLocation(new LocationImmutable(this.getFormattedWorldName(theme), null, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()))
            .environment(Environment.valueOf(theme.getStats().environment()))
            .build();

        final World world = BukkitWorldCreator.getOrCreate(params);
        plugin.getLogger().info("Loaded world: " + world.getName() + " in " + stopwatch.stop());

        WORLDS.add(world.getName());
        return world;
    }

    @Internal
    private void paste(@NotNull Skin skin, @NotNull String fileName, @NotNull World world, @NotNull InputStream inputStream, BukkitPlugin plugin) {
        final long start = System.currentTimeMillis();
        final String[] splitExtension = fileName.split("\\.");
        if (splitExtension.length < 2) {
            plugin.getLogger().info("Could not find extension for: " + fileName + ". Skipping it.");
            return;
        }

        final ClipboardFormat format = ClipboardFormats.findByExplicitExtension(SCHEMATIC_EXTENSION);
        if (format == null) {
            return;
        }

        final Location location = world.getSpawnLocation();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        BukkitWorld bukkitWorld;

        final WorldEdit worldEdit = WorldEdit.getInstance();

        try (final Clipboard clipboard = this.getClipboard(inputStream)) {
            bukkitWorld = new BukkitWorld(world);

            try (final EditSession editSession = worldEdit.newEditSessionBuilder().world(bukkitWorld).limitUnlimited().fastMode(true).build();
                final ClipboardHolder holder = new ClipboardHolder(clipboard)) {

                final Operation operation = holder.createPaste(editSession)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(true)
                    .copyEntities(false)
                    .copyBiomes(false)
                    .build();

                Operations.complete(operation);
                plugin.getLogger().info("Skin %s pasted successfully in %sms.".formatted(fileName, System.currentTimeMillis() - start));
            }
        } finally {
            skin.setReady(true);
            bukkitWorld = null; // Safe cleanup world reference
        }
    }

    @Internal
    private @NotNull Clipboard getClipboard(InputStream inputStream) {
        final ClipboardFormat format = ClipboardFormats.findByExplicitExtension(SCHEMATIC_EXTENSION);
        if (format == null) {
            throw new IllegalArgumentException("Unsupported schematic format");
        }

        try (final ClipboardReader reader = format.getReader(inputStream)) {
            return reader.read(
                UUID.randomUUID(),
                dimensions -> new CPUOptimizedClipboard(new CuboidRegion(
                    null,
                    BlockVector3.ZERO,
                    dimensions.subtract(BlockVector3.ONE),
                    false
                ))
            );
        } catch (IOException exception) {
            throw new RuntimeException("Could not read clipboard", exception);
        }
    }
}

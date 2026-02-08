package io.github.minehollow.lobby.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public final class HologramHandler {
    private WrapperEntity entity;
    private HologramData data;
    private final MiniMessage miniMessage;

    public HologramHandler(@NotNull HologramData data, @NotNull MiniMessage miniMessage) {
        this.data = data;
        this.miniMessage = miniMessage;
        setupEntity();
    }

    private void setupEntity() {
        entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        var meta = (TextDisplayMeta) entity.getEntityMeta();

        Component text = miniMessage.deserialize(data.getCombinedLines());
        meta.setText(text);
        meta.setBillboardConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(0x0);
    }

    public void spawn() {
        var location = SpigotConversionUtil.fromBukkitLocation(data.location());
        entity.spawn(location);

        for (Player player : Bukkit.getOnlinePlayers()) {
            addViewer(player);
        }
    }

    public void remove() {
        entity.remove();
    }

    public void teleport(@NotNull Location location) {
        this.data = new HologramData(data.id(), location, data.lines());
        var peLocation = SpigotConversionUtil.fromBukkitLocation(location);
        entity.teleport(peLocation);
    }

    public void updateLines(@NotNull List<String> lines) {
        this.data = new HologramData(data.id(), data.location(), lines);

        var meta = (TextDisplayMeta) entity.getEntityMeta();
        Component text = miniMessage.deserialize(String.join("\n", lines));
        meta.setText(text);
        entity.refresh();
    }

    public void addViewer(@NotNull Player player) {
        entity.addViewer(player.getUniqueId());
    }

    public void removeViewer(@NotNull Player player) {
        entity.removeViewer(player.getUniqueId());
    }
}
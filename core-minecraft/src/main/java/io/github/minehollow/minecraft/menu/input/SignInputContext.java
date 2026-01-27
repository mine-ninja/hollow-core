package io.github.minehollow.minecraft.menu.input;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.input.SignInputMenu.Response;
import io.github.minehollow.minecraft.util.reflection.McVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

@Getter
public class SignInputContext extends PlayerMenuContext {
    private final Location location;
    
    public SignInputContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
        Location origin = player.getEyeLocation().clone();
        Vector direction = origin.getDirection();
        direction = direction.setY(0);
        direction = direction.normalize().multiply(1);
        this.location = origin.subtract(direction);
        
        if (!rawData.containsKey("lines")) {
            rawData.put("lines", new Component[0]);
        }
    }
    
    public void accept() {
        Response response = this.getOrDefault("response", Response.EMPTY);
        response.handler.accept(this);
    }
    
    @Override
    public void update() { }
    
    @Override
    public void open() {
        if (!McVersion.supports(21)) {
            throw new UnsupportedOperationException("Sign input menu is only supported on Minecraft 1.21 or higher.");
        }
        
        PlayerManager manager = PacketEvents.getAPI().getPlayerManager();
        
        Vector3i pos = new Vector3i(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
        manager.sendPacket(this.player, new WrapperPlayServerBlockChange(pos, SpigotConversionUtil.fromBukkitBlockData(Material.BIRCH_SIGN.createBlockData())));
        
        NBTCompound tag = new NBTCompound();
        tag.setTag("x", new NBTInt(pos.getX()));
        tag.setTag("y", new NBTInt(pos.getY()));
        tag.setTag("z", new NBTInt(pos.getZ()));
        tag.setTag("id", new NBTString("minecraft:birch_sign"));
        
        if (tag.getCompoundTagOrNull("front_text") == null) {
            tag.setTag("front_text", new NBTCompound());
        }
        
        NBTCompound frontText = tag.getCompoundTagOrThrow("front_text");
        if (tag.getStringListTagOrNull("messages") == null) {
            frontText.setTag("messages", new NBTList<>(NBTType.STRING));
        }
        
        Component[] lines = this.getOrDefault("lines", new Component[0]);
        NBTList<@NotNull NBTString> messages = frontText.getStringListTagOrThrow("messages");
        for (int i = 0; i < 4; i++) {
            String gson = GsonComponentSerializer.gson().serialize(i >= lines.length ? Component.empty() : lines[i]);
            messages.addTag(new NBTString(gson));
        }
        
        manager.sendPacket(this.player, new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, tag));
        manager.sendPacket(this.player, new WrapperPlayServerOpenSignEditor(pos, true));
    }
}

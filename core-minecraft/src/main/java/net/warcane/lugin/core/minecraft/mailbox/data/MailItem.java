package net.warcane.lugin.core.minecraft.mailbox.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.warcane.lugin.core.minecraft.mailbox.utils.ItemSerializer;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
@Data
public class MailItem {
    private final UUID mailId;
    private final String serverId;
    private String serializedContents;

    @BsonIgnore
    private ItemStack[] contents;

    @BsonCreator
    public MailItem(
        @BsonProperty("mailId") UUID mailId,
        @BsonProperty("serverId") String serverId,
        @BsonProperty("serializedContents") String serializedContents) {
        this.mailId = mailId;
        this.serverId = serverId;
        this.serializedContents = serializedContents;

        this.contents = ItemSerializer.deserializeList(serializedContents);
    }

    public MailItem(String serverId, ItemStack[] contents) {
        mailId = UUID.randomUUID();
        this.serverId = serverId;
        this.serializedContents = ItemSerializer.serialize(contents);
        this.contents = contents;
    }

    @BsonProperty
    public ItemStack getDisplayItem() {
        if (contents.length == 0) {
            return new ItemStack(Material.BARRIER);
        }
        return contents[0];
    }

    @BsonProperty
    public boolean canAddToPlayerInv(PlayerInventory inventory) {
        int emptySlots = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        if (emptySlots <= 0) {
            return false;
        }
        return emptySlots > contents.length;
    }


    @NotNull
    @Contract("_, _ -> new")
    public static MailItem create(String serverId, ItemStack[] contents) {
        return new MailItem(serverId, contents);
    }
}

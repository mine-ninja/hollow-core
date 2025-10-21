package net.warcane.lugin.core.minecraft.mailbox.utils;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ItemSerializer {
	public static final Codec<ItemStack> CODEC = Codec.STRING.xmap(ItemSerializer::deserialize, ItemSerializer::serialize);
	
	public static String serialize(ItemStack item) {
		if (item == null) { return null; }
		
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
			
			dataOutput.writeObject(item);
			dataOutput.close();
			
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize ItemStack", e);
		}
	}

    public static String serialize(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream)) {

            bukkitOutputStream.writeInt(items.length);
            for (ItemStack itemStack : items) {
                bukkitOutputStream.writeObject(itemStack);
            }

            bukkitOutputStream.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize ItemStack array", e);
        }
    }

    public static ItemStack[] deserializeList(String data) {
        if (data.isEmpty()) return new ItemStack[0];

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream); ) {

            ItemStack[] itemStacks = new ItemStack[bukkitInputStream.readInt()];
            for (int slot = 0; slot < itemStacks.length; slot++) {
                itemStacks[slot] = (ItemStack) bukkitInputStream.readObject();
            }

            bukkitInputStream.close();
            return itemStacks;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize ItemStack array", e);
        }
    }
	
	public static ItemStack deserialize(String data) {
		if (data == null || data.isEmpty()) { return null; }
		
		try {
			byte[] bytes = Base64.getDecoder().decode(data);
			
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
			BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			
			ItemStack item = (ItemStack) dataInput.readObject();
			dataInput.close();
			
			return item;
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize ItemStack", e);
		}
	}
}

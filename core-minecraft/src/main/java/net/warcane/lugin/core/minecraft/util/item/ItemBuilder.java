package net.warcane.lugin.core.minecraft.util.item;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.warcane.lugin.core.minecraft.util.GameProfileUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ItemBuilder {

    public static ItemBuilder createSkull() {
        return new ItemBuilder(Material.SKULL_ITEM, 1, 3);
    }

    private final ItemStack itemStack;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(Material material, int amount, int durability) {
        this(new ItemStack(material, amount, (short) durability));
    }

    public ItemBuilder(MaterialData materialData) {
        this(materialData.toItemStack(1));
    }

    public ItemBuilder name(@NotNull String name) {
        return useMeta(itemMeta -> itemMeta.setDisplayName(name));
    }

    public ItemBuilder lore(@NotNull String... lore) {
        return useMeta(itemMeta -> itemMeta.setLore(List.of(lore)));
    }

    public ItemBuilder lore(@NotNull List<String> lore) {
        return useMeta(itemMeta -> itemMeta.setLore(lore));
    }

    public ItemBuilder flag(ItemFlag... flags) {
        return useMeta(itemMeta -> itemMeta.addItemFlags(flags));
    }

    public ItemBuilder useItem(@NotNull Consumer<ItemStack> consumer) {
        consumer.accept(itemStack);
        return this;
    }

    public ItemBuilder useMeta(@NotNull Consumer<ItemMeta> itemMetaConsumer) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMetaConsumer.accept(itemMeta);
            itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    public ItemBuilder customSkull(@NotNull Player player) {
        try {
            GameProfile profile = GameProfileUtil.getGameProfile(player);
            Iterator<Property> textures = profile.getProperties().get("textures").iterator();
            if (textures.hasNext()) {
                textures.next();
                return customSkull(profile);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return this;
    }

    public ItemBuilder customSkull(@NotNull String value) {
        if (StringUtils.isEmpty(value))
            return this;

        GameProfile profile = new GameProfile(UUID.randomUUID(), value);
        profile.getProperties().put("textures", new Property("textures", value));
        return customSkull(profile);
    }

    public ItemBuilder customSkullUrl(@NotNull String url) {
        if (StringUtils.isEmpty(url))
            return this;

        if (!url.startsWith("http://textures.minecraft.net/texture/"))
            url = "http://textures.minecraft.net/texture/" + url;
        GameProfile profile = new GameProfile(UUID.randomUUID(), url);
        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        return customSkull(profile);
    }

    private ItemBuilder customSkull(@NotNull GameProfile profile) {
        SkullMeta meta = (SkullMeta) this.itemStack.getItemMeta();
        Field profileField;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException |
                 SecurityException exception) {
            exception.printStackTrace();
        }
        this.itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }
}

package io.github.minehollow.minecraft.util.item;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.message.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Rok, Pedro Lucas nmm. Created on 17/07/2025
 * @project factions-essentials
 */
public class ItemBuilder {
    // TODO - Move to CORE
    public static final NamespacedKey MENU_KEY = new NamespacedKey(BukkitPlatform.getInstance().getPlugin(), "menu_item");

    private ItemStack itemStack;
    private List<Component> finalLore = new ArrayList<>();

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemBuilder() {
        this.itemStack = new ItemStack(Material.AIR);
    }

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
    }

    /**
     * Cria um ItemBuilder com um modelo personalizado. (usa o material do Echo Shard)
     */
    public ItemBuilder(int customModel) {
        this.itemStack = new ItemStack(Material.ECHO_SHARD);
        this.itemStack.editMeta(meta -> {
            meta.setCustomModelData(customModel);
            meta.getPersistentDataContainer().set(MENU_KEY, PersistentDataType.BOOLEAN, true);
        });
    }

    public ItemBuilder type(Material material) {
        itemStack = this.itemStack.withType(material);
        return this;
    }

    /**
     * Nome do item (Use a formatação do adventure)
     */
    public ItemBuilder name(String name) {
        itemStack.editMeta(meta -> meta.displayName(StringUtils.formItemName(name, false, true)));
        return this;
    }

    public ItemBuilder name(Component name) {
        itemStack.editMeta(meta -> meta.displayName(name));
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * @param lore Lore do item (Use a formatação do adventure)
     */
    public ItemBuilder lore(String... lore) {
        finalLore = new ArrayList<>();
        for (String s : lore) {
            finalLore.add(StringUtils.formItemName(s, false, true));
        }
        return this;
    }

    /**
     * @param lore Lore do item (Use a formatação do adventure)
     */
    public ItemBuilder lore(List<String> lore) {
        finalLore = new ArrayList<>();
        for (String s : lore) {
            finalLore.add(StringUtils.formItemName(s, false, true));
        }
        return this;
    }

    /**
     * @param lore Adiciona lore ao item (Use a formatação do adventure)
     */
    public ItemBuilder addLore(String lore) {
        finalLore.add(StringUtils.formItemName(lore, false, true));
        return this;
    }

    public ItemBuilder addLore(Component lore) {
        finalLore.add(lore);
        return this;
    }

    /**
     * @param lore Adiciona lore ao item (Use a formatação do adventure)
     */
    public ItemBuilder addLore(String... lore) {
        for (String s : lore) {
            finalLore.add(StringUtils.formItemName(s, false, true));
        }
        return this;
    }

    public ItemBuilder custom(int customModelData) {
        itemStack.editMeta(meta -> meta.setCustomModelData(customModelData));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        itemStack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder removeEnchant(Enchantment enchantment) {
        itemStack.removeEnchantment(enchantment);
        return this;
    }

    public ItemBuilder removeAllEnchant() {
        itemStack.removeEnchantments();
        return this;
    }

    public ItemBuilder meta(Consumer<ItemMeta> consumer) {
        itemStack.editMeta(consumer);
        return this;
    }

    public <M extends ItemMeta> ItemBuilder meta(final @NotNull Class<M> metaClass, Consumer<M> consumer) {
        itemStack.editMeta(metaClass, consumer);
        return this;
    }

    public ItemBuilder removeAllFlags() {
        itemStack.removeItemFlags();
        return this;
    }

    public ItemBuilder flags(@NotNull ItemFlag... flags) {
        itemStack.addItemFlags(flags);
        return this;
    }

    public ItemBuilder glow() {
        itemStack.editMeta(meta -> {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        });
        return this;
    }

    public ItemBuilder menuItem() {
        itemStack.editMeta(meta -> meta.getPersistentDataContainer().set(MENU_KEY, PersistentDataType.BOOLEAN, true));
        return this;
    }

    public ItemBuilder customSkull(@NotNull Player player) {
        itemStack.editMeta(SkullMeta.class, meta ->
          meta.setOwningPlayer(player)
        );
        return this;
    }

    public ItemBuilder customSkull(@NotNull String value) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), value);
        profile.getProperties().put("textures", new Property("textures", value));
        return this.customSkull(profile);
    }

    public ItemBuilder customSkullUrl(@NotNull String url) {
        if (!url.startsWith("http://textures.minecraft.net/texture/")) {
            url = "http://textures.minecraft.net/texture/" + url;
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), url);
        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        return this.customSkull(profile);
    }

    private ItemBuilder customSkull(@NotNull GameProfile profile) {
        //SkullMeta meta = (SkullMeta) this.itemStack.getItemMeta();
//
        //try {
        //    Field profileField = meta.getClass().getDeclaredField("profile");
        //    profileField.setAccessible(true);
        //    profileField.set(meta, profile);
        //} catch (IllegalAccessException | NoSuchFieldException | SecurityException |
        //         IllegalArgumentException exception) {
        //    ((Exception) exception).printStackTrace();
        //}

        //this.itemStack.setItemMeta(meta);
        return this;
    }

    public static ItemBuilder skull() {
        return new ItemBuilder(Material.PLAYER_HEAD);
    }

    public static ItemBuilder skull(@NotNull Player player) {
        return new ItemBuilder(Material.PLAYER_HEAD).customSkull(player);
    }

    public static ItemBuilder skull(@NotNull String value) {
        return new ItemBuilder(Material.PLAYER_HEAD).customSkull(value);
    }

    public ItemStack build() {
        if (Material.AIR.equals(itemStack.getType())) return itemStack;
        itemStack.lore(finalLore);
        return itemStack;
    }

    public ItemStack get() {
        return build();
    }

    public static ItemBuilder of() {
        return new ItemBuilder();
    }

    public static ItemBuilder of(ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    /**
     * Cria um ItemBuilder com um modelo personalizado. (usa o material do Echo Shard)
     */
    public static ItemBuilder of(int customModel) {
        return new ItemBuilder(customModel);
    }
}

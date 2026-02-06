package io.github.minehollow.kits.util;

import io.github.minehollow.minecraft.util.item.ItemBuilder;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


@UtilityClass
public class MenuItemsUtil {
    public final ItemStack SEPARATOR = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();

    public final ItemStack BACK_BUTTON = ItemBuilder.of(Material.ARROW)
            .name("<gray>Voltar")
            .lore("", "<dark_gray>Clique para voltar")
            .build();

    public final ItemStack SAVE_BUTTON = ItemBuilder.of(Material.LIME_DYE)
            .name("<green>Salvar")
            .lore("", "<gray>Clique para salvar", "<gray>todas as alterações.")
            .build();

    public final ItemStack DELETE_BUTTON = ItemBuilder.of(Material.RED_DYE)
            .name("<red>Deletar")
            .lore("", "<gray>Clique para deletar", "<gray>permanentemente!", "", "<dark_red>Esta ação é irreversível!")
            .build();

}

package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KitCategoryMenu extends SimpleMenu {
    private static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
        .name(" ")
        .build();

    private final KitService kitService;

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        config.setTitle(StringUtils.text("<gradient:#E0AAFF:#9D4EDD><bold>Categorias de Kits"));
        config.setLayout(
            "XXXXXXXXX",
            "XCCCCCCCX",
            "XXXXXXXXX");
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        ctx.setItem('X', BORDER);

        List<KitCategory> categories = kitService.getAllCategories().stream()
            .sorted(Comparator.comparingInt(KitCategory::getPriority))
            .toList();

        int[] slots = config.getLayout().get('C');
        for (int i = 0; i < slots.length && i < categories.size(); i++) {
            KitCategory category = categories.get(i);
            int kitCount = kitService.getKitsByCategory(category.getId()).size();

            ItemStack icon = ItemBuilder
                .of(category.getIcon() != null ? category.getIcon() : Material.CHEST)
                .name("<white><bold>" + category.getDisplayName())
                .lore(
                    "",
                    "<gray>Kits: <white>" + kitCount,
                    "",
                    "<dark_gray>Clique para ver")
                .build();

            ctx.setItem(slots[i], icon, e -> openCategoryKits(ctx, category));
        }

        return true;
    }

    private void openCategoryKits(PlayerMenuContext ctx, KitCategory category) {
        ctx.openMenu(KitListMenu.class, Map.of("categoryId", category.getId()));
    }
}

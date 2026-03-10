package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.kits.util.MenuItemsUtil;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class KitCategoryMenu extends SimpleMenu {
    private static final int ITEMS_PER_ROW = 3;
    private static final int MAX_CONTENT_ROWS = 4;
    private static final String BORDER_ROW = "XXXXXXXXX";
    private static final String CATEGORY_ROW = "XXCXCXCXX";

    private final KitService kitService;

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        config.setTitle(StringUtils.text("Categorias de Kits"));
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        List<KitCategory> categories = kitService.getAllCategories()
            .stream()
            .sorted(Comparator.comparingInt(KitCategory::getPriority))
            .toList();

        config.setLayout(buildLayout(categories.size()));

        int[] slots = config.getLayout().get('C');
        for (int i = 0; i < slots.length; i++) {
            if (i >= categories.size()) {
                ctx.setItem(slots[i], MenuItemsUtil.COMING_SOON);
                continue;
            }

            KitCategory category = categories.get(i);
            ItemStack icon = ItemBuilder
                .of(category.getIcon() != null ? category.getIcon() : Material.CHEST)
                .name(category.getDisplayName().toUpperCase())
                .lore(
                    "",
                    "<gray>Visualize os kits",
                    "<gray>disponíveis nesta categoria.",
                    "",
                    "<yellow>Clique para ver"
                )
                .build();

            ctx.setItem(slots[i], icon, e -> openCategoryKits(ctx, category));
        }

        return true;
    }

    private String[] buildLayout(int categoryCount) {
        int contentRows = Math.max(1, Math.min(MAX_CONTENT_ROWS, (categoryCount + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW));
        String[] layout = new String[contentRows + 2];
        layout[0] = BORDER_ROW;
        Arrays.fill(layout, 1, contentRows + 1, CATEGORY_ROW);
        layout[layout.length - 1] = BORDER_ROW;
        return layout;
    }

    private void openCategoryKits(PlayerMenuContext ctx, KitCategory category) {
        ctx.openMenu(KitListMenu.class, Map.of("categoryId", category.getId()));
    }
}

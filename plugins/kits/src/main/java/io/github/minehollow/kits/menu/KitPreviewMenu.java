package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.util.MenuItemsUtil;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class KitPreviewMenu extends SimpleMenu {

    public KitPreviewMenu() {
        defaultConfig.setLayout(
                "IIIIIIIII",
                "IIIIIIIII",
                "IIIIIIIII",
                "IIIIIIIII",
                "----B----");
        defaultConfig.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Player player = ctx.getPlayer();
        Kit kit = ctx.get("kit");

        if (kit == null) {
            player.sendMessage(StringUtils.text("<red>Kit não encontrado!"));
            return false;
        }

        config.setTitle(StringUtils.text("Visualizando kit: " + kit.getDisplayName()));

        List<ItemStack> items = kit.getItems();
        int[] slots = config.getLayout().get('I');

        for (int i = 0; i < slots.length; i++) {
            if (i < items.size() && items.get(i) != null) {
                ctx.setItem(slots[i], items.get(i).clone());
            }
        }

        // SEPARATOR
        ctx.setItem('-', MenuItemsUtil.SEPARATOR, e -> e.setCancelled(true));

        // BACK
        ctx.setItem('B', MenuItemsUtil.BACK_BUTTON, e -> {
            String categoryId = kit.getCategoryId();
            if (categoryId != null) {
                ctx.openMenu(KitListMenu.class, Map.of("categoryId", categoryId));
            } else {
                ctx.openMenu(KitListMenu.class);
            }
        });
        return true;
    }
}

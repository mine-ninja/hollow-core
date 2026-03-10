package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.util.MenuItemsUtil;
import io.github.minehollow.minecraft.menu.MenuContext;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.sdk.util.time.Time;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KitListMenu extends SimpleMenu {
    private static final int ITEMS_PER_ROW = 3;
    private static final int MIN_CONTENT_ROWS = 2;
    private static final int MAX_CONTENT_ROWS = 4;
    private static final String BORDER_ROW = "XXXXXXXXX";
    private static final String KIT_ROW = "XXKXKXKXX";
    private static final String FOOTER_ROW = "XXXXXXXXX";
    private static final String FOOTER_ROW_WITH_BACK = "BXXXXXXXX";

    private final KitService kitService;

    public KitListMenu(KitService kitService) {
        this.kitService = kitService;
        this.defaultConfig.setTickUpdateEnabled(true);
        this.defaultConfig.setUpdateIntervalMillis(1, TimeUnit.SECONDS);
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        String categoryId = ctx.get("categoryId");
        String title = categoryId != null
                ? "Kits: " + getDisplayName(categoryId)
                : "Kits Disponíveis";

        config.setTitle(StringUtils.text(title));
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        List<Kit> kits = categoryId != null
                ? kitService.getKitsByCategory(categoryId)
                : kitService.getAllKitsSync();

        config.setLayout(buildLayout(kits.size(), categoryId != null));
        ctx.put("kits", kits);

        int[] slots = config.getLayout().get('K');
        for (int i = 0; i < slots.length; i++) {
            if (i >= kits.size()) {
                ctx.setItem(slots[i], MenuItemsUtil.COMING_SOON);
                continue;
            }

            Kit kit = kits.get(i);
            ctx.setItem(slots[i], p -> renderKitIcon(p, kit), e -> {
                e.setCancelled(true);
                if (e.isRightClick()) {
                    ctx.openMenu(KitPreviewMenu.class, Map.of("kit", kit));
                } else {
                    handleKitClick(ctx, kit);
                }
            });
        }

        if (categoryId != null) {
            ctx.setItem('B', MenuItemsUtil.BACK_BUTTON, e -> {
                e.setCancelled(true);
                ctx.openMenu(KitCategoryMenu.class);
            });
        }

        return true;
    }

    @Override
    protected void onTick(@NotNull MenuContext ctx) {
        if (!(ctx instanceof PlayerMenuContext playerCtx))
            return;

        List<Kit> kits = playerCtx.get("kits");
        if (kits == null)
            return;

        int[] slots = playerCtx.getMenuConfig().getLayout().get('K');
        Player player = playerCtx.getPlayer();

        for (int i = 0; i < slots.length && i < kits.size(); i++) {
            Kit kit = kits.get(i);
            playerCtx.getInventory().setItem(slots[i], renderKitIcon(player, kit));
        }
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private String getDisplayName(String categoryId) {
        var category = kitService.getCategory(categoryId);
        if (category == null)
            return categoryId;
        // Remove MiniMessage tags like <red>, <bold>, etc.
        return category.getDisplayName().replaceAll("<[^>]+>", "");
    }

    private String[] buildLayout(int kitCount, boolean hasBackButton) {
        int contentRows = Math.max(MIN_CONTENT_ROWS, Math.min(MAX_CONTENT_ROWS, (kitCount + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW));
        String[] layout = new String[contentRows + 2];
        layout[0] = BORDER_ROW;
        Arrays.fill(layout, 1, contentRows + 1, KIT_ROW);
        layout[layout.length - 1] = hasBackButton ? FOOTER_ROW_WITH_BACK : FOOTER_ROW;
        return layout;
    }

    private ItemStack renderKitIcon(Player player, Kit kit) {
        boolean hasPerm = kitService.hasPermission(player, kit);
        long remaining = kitService.getCachedRemainingTime(player.getUniqueId(), kit.getId());
        boolean onCooldown = remaining > 0;

        Material icon = kit.getIcon() != null ? kit.getIcon() : Material.CHEST;
        String nameColor = !hasPerm ? "<red>" : onCooldown ? "<gray>" : "<white>";
        String cooldownText = kit.hasCooldown() ? new Time(kit.getCooldown(), TimeUnit.SECONDS).toString() : "Nenhum";

        String statusLine;
        if (!hasPerm) {
            statusLine = "<red>Sem permissão";
        } else if (onCooldown) {
            String timeLeft = new Time(remaining, TimeUnit.SECONDS).toString();
            statusLine = "<yellow>Disponível em: " + timeLeft;
        } else {
            statusLine = "<green>Clique para coletar";
        }

        return ItemBuilder.of(icon).name(nameColor + kit.getDisplayName())
                .lore(
                        "",
                        "<gray>Cooldown: <white>" + cooldownText,
                        "",
                        statusLine,
                        "",
                        "<dark_gray>Esquerdo: <gray>Coletar",
                        "<dark_gray>Direito: <gray>Preview")
                .flags(ItemFlag.values())
                .build();
    }

    private void handleKitClick(PlayerMenuContext ctx, Kit kit) {
        Player player = ctx.getPlayer();

        kitService.claimKit(player, kit).thenAccept(result -> Tasks.runSync(() -> {
            switch (result.status()) {
                case SUCCESS -> {
                    player.closeInventory();
                    player.sendMessage(
                            StringUtils.text("<green>Você coletou o kit <white>" + kit.getDisplayName()
                                    + "<green>!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                }
                case NO_PERMISSION -> {
                    player.sendMessage(StringUtils.text("<red>Você não tem permissão para usar este kit!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                case ON_COOLDOWN -> {
                    player.sendMessage(StringUtils.text(
                            "<gray>Este kit estará disponível em: <yellow>"
                                    + result.getFormattedRemainingTime()));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                case NO_SPACE -> {
                    player.sendMessage(
                            StringUtils.text("<red>Você não tem espaço suficiente no inventário!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                case NOT_FOUND -> {
                    player.sendMessage(StringUtils.text("<red>Kit não encontrado!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }));
    }
}

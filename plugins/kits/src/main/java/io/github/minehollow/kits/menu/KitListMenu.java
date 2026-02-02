package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.Kit;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KitListMenu extends SimpleMenu {
    private static final ItemStack BACK_BUTTON = ItemBuilder.of(Material.ARROW)
            .name("<gray>Voltar")
            .lore("", "<dark_gray>Voltar às categorias")
            .build();

    private final KitService kitService;

    public KitListMenu(KitService kitService) {
        this.kitService = kitService;
        this.defaultConfig.setTickUpdateEnabled(true);
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        String categoryId = ctx.get("categoryId");
        String title = categoryId != null
                ? "Kits: " + getDisplayName(categoryId)
                : "Kits Disponíveis";

        config.setTitle(StringUtils.text(title));
        config.setLayout(
                "XXXXXXXXX",
                "XXKXKXKXX",
                "XXXXBXXXX");
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        Player player = ctx.getPlayer();
        List<Kit> kits = categoryId != null
                ? kitService.getKitsByCategory(categoryId)
                : kitService.getAllKits().join();
        kitService.loadPlayerData(player.getUniqueId()).join();

        ctx.put("kits", kits);

        int[] slots = config.getLayout().get('K');
        for (int i = 0; i < slots.length && i < kits.size(); i++) {
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
            ctx.setItem('B', BACK_BUTTON, e -> {
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
        return category != null ? category.getDisplayName() : categoryId;
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

        return ItemBuilder.of(icon)
                .name(nameColor + kit.getDisplayName())
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

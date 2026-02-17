package io.github.minehollow.mailbox.menu;

import io.github.minehollow.mailbox.MailboxPlugin;
import io.github.minehollow.mailbox.model.MailboxItem;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.pagination.MenuPaginationContext;
import io.github.minehollow.minecraft.menu.pagination.SimplePaginationMenu;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BoxPreviewMenu extends SimplePaginationMenu<ItemStack> {
        private static final PredefinedSound CLAIM_SOUND = new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.0F);
        private static final PredefinedSound ERROR_SOUND = new PredefinedSound(Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        private final MailboxPlugin plugin;

        @Override
        public boolean onPreOpen(@NotNull MenuPaginationContext<ItemStack> ctx, @NotNull MenuConfig openHandler) {
                final Player player = ctx.getPlayer();
                final var service = plugin.getMailboxService();
                final var data = service.getCachedData(player.getUniqueId());

                if (data == null) {
                        StringUtils.send(player, "<red>Erro ao carregar dados. Tente novamente.");
                        return false;
                }

                String boxId = ctx.get("boxId");
                if (boxId == null) {
                        StringUtils.send(player, "<red>Caixa não encontrada.");
                        return false;
                }

                MailboxItem box = data.getBoxById(boxId);
                if (box == null) {
                        StringUtils.send(player, "<red>Caixa não encontrada.");
                        return false;
                }

                openHandler.setTitle(StringUtils.text("Pré-visualização"));
                openHandler.setLayout(
                                "    H    ",
                                " IIIIIII ",
                                " IIIIIII ",
                                " IIIIIII ",
                                "P       N",
                                " V  R  B ");

                openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));

                String date = DATE_FORMAT.format(new Date(box.getSentAt()));

                ctx.setItem('H', ItemBuilder.of(Material.CHEST)
                                .name("<white>" + box.getDescription())
                                .addLore(
                                                "",
                                                " <gray>De: <white>" + box.getSenderPlugin(),
                                                " <gray>Data: <white>" + date,
                                                " <gray>Itens: <white>" + box.getItemCount())
                                .build());

                ctx.setItem('R', ItemBuilder.of(Material.LIME_DYE)
                                .name("<green><bold>RESGATAR</bold>")
                                .addLore(
                                                "",
                                                " <gray>Resgata os itens desta caixa.",
                                                " <gray>Itens sem espaço cairão no chão.",
                                                "",
                                                " <yellow>Clique para resgatar!")
                                .build(),
                                event -> {
                                        boolean success = plugin.getMailboxService().claimBox(player, boxId);
                                        if (success) {
                                                StringUtils.send(player, "<green><bold>RESGATADO!</bold> <gray>"
                                                                + box.getDescription());
                                                CLAIM_SOUND.play(player);
                                                ctx.openMenu(MailboxMenu.class);
                                        } else {
                                                StringUtils.send(player, "<red>Erro ao resgatar esta entrega.");
                                                ERROR_SOUND.play(player);
                                        }
                                });

                ctx.setItem('V', ItemBuilder.of(Material.DARK_OAK_DOOR)
                                .name("<red>Voltar")
                                .addLore(" <gray>Voltar ao correio.")
                                .build(),
                                event -> ctx.openMenu(MailboxMenu.class));

                ctx.setItem('B', ItemBuilder.of(Material.DARK_OAK_DOOR)
                                .name("<red>Voltar")
                                .addLore(" <gray>Voltar ao correio.")
                                .build(),
                                event -> ctx.openMenu(MailboxMenu.class));

                return true;
        }

        @Override
        protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player,
                        @NotNull Map<String, Object> initialData) {
                final var openHandler = new MenuConfig(defaultConfig);
                final MenuPaginationContext<ItemStack> ctx = new MenuPaginationContext<>(player, initialData,
                                openHandler, this, manager);
                if (!this.onPreOpen(ctx, openHandler))
                        return;

                ctx.initialize();

                // setPagination must be called AFTER initialize() because it triggers update()
                String boxId = ctx.get("boxId");
                var data = plugin.getMailboxService().getCachedData(player.getUniqueId());
                if (data != null && boxId != null) {
                        MailboxItem box = data.getBoxById(boxId);
                        if (box != null) {
                                List<ItemStack> items = Arrays.stream(box.getItems())
                                                .filter(item -> item != null && !item.getType().isAir())
                                                .toList();

                                ctx.setPagination('I', items,
                                                (p, item) -> item,
                                                (item, event) -> event.setCancelled(true));

                                ctx.setPreviousButton('P', ItemBuilder.of(Material.ARROW)
                                                .name("<yellow>Página Anterior")
                                                .build());

                                ctx.setNextButton('N', ItemBuilder.of(Material.ARROW)
                                                .name("<yellow>Próxima Página")
                                                .build());
                        }
                }

                playerContexts.put(player.getUniqueId(), ctx);
                ctx.open();
        }
}

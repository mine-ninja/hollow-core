package io.github.minehollow.quests.menu;

import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.quests.QuestsPlugin;
import io.github.minehollow.quests.quest.QuestTemplate;
import io.github.minehollow.quests.quest.QuestType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QuestCreateMenu extends SimpleMenu {
    private static final PredefinedSound SUCCESS_SOUND = new PredefinedSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F,
            1.0F);
    private static final PredefinedSound ERROR_SOUND = new PredefinedSound(Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);

    private static final String KEY_TYPE = "quest_type";
    private static final String KEY_FILTER = "quest_filter";
    private static final String KEY_DISPLAY = "quest_display";

    private final QuestsPlugin plugin;

    public QuestCreateMenu(QuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        final Player player = ctx.getPlayer();

        if (!player.hasPermission("quests.admin")) {
            StringUtils.send(player, "<red>Sem permissão.");
            return false;
        }

        openHandler.setTitle(StringUtils.text("Criar Quest"));
        openHandler.setRows(3);
        openHandler.setLayout(
                "         ",
                "  TFD S  ",
                "         ");

        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));

        if (ctx.get(KEY_TYPE) == null) {
            ctx.put(KEY_TYPE, QuestType.BLOCK_BREAK);
            ctx.put(KEY_FILTER, "");
            ctx.put(KEY_DISPLAY, "Nova Quest");
        }

        ctx.setItem('T', p -> {
            QuestType type = ctx.getOrDefault(KEY_TYPE, QuestType.BLOCK_BREAK);
            return ItemBuilder.of(type.getIcon())
                    .name("<yellow>Tipo: <white>" + type.getDisplayName())
                    .addLore(
                            "",
                            " <gray>Clique para trocar",
                            " <dark_gray>Atual: " + type.name())
                    .build();
        }, event -> {
            QuestType current = ctx.getOrDefault(KEY_TYPE, QuestType.BLOCK_BREAK);
            QuestType[] values = QuestType.values();
            int next = (current.ordinal() + 1) % values.length;
            ctx.put(KEY_TYPE, values[next]);
            ctx.update();
        });

        ctx.setItem('F', p -> {
            String filter = ctx.getOrDefault(KEY_FILTER, "");
            QuestType type = ctx.getOrDefault(KEY_TYPE, QuestType.BLOCK_BREAK);
            String hint = type == QuestType.BLOCK_BREAK || type == QuestType.FISHING
                    ? "Ex: STONE, DIAMOND_ORE"
                    : "Ex: ZOMBIE, SKELETON";
            return ItemBuilder.of(Material.HOPPER)
                    .name("<yellow>Filtro: <white>" + (filter.isEmpty() ? "Qualquer" : filter))
                    .addLore(
                            "",
                            " <gray>Material ou tipo de mob",
                            " <gray>Deixe vazio para qualquer",
                            " <dark_gray>" + hint,
                            "",
                            " <green>Clique para definir")
                    .build();
        }, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();

            ChatInput.waitInput(p, input -> {
                if (!input.equalsIgnoreCase("cancelar")) {
                    ctx.put(KEY_FILTER, input.trim().toUpperCase());
                }
                reopenMenu(p, ctx);
            }, "<yellow>Digite o filtro no chat <gray>(material ou mob). <red>\"cancelar\" para voltar.");
        });

        ctx.setItem('D', p -> {
            String display = ctx.getOrDefault(KEY_DISPLAY, "Nova Quest");
            return ItemBuilder.of(Material.NAME_TAG)
                    .name("<yellow>Nome: <white>" + display)
                    .addLore(
                            "",
                            " <green>Clique para definir")
                    .build();
        }, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();

            ChatInput.waitInput(p, input -> {
                if (!input.equalsIgnoreCase("cancelar") && !input.trim().isEmpty()) {
                    ctx.put(KEY_DISPLAY, input.trim());
                }
                reopenMenu(p, ctx);
            }, "<yellow>Digite o nome da quest no chat. <red>\"cancelar\" para voltar.");
        });

        ctx.setItem('S', ItemBuilder.of(Material.EMERALD_BLOCK)
                .name("<green><bold>SALVAR QUEST")
                .addLore(
                        "",
                        " <gray>Clique para criar a quest",
                        "",
                        " <dark_gray>Quantidade e recompensas",
                        " <dark_gray>são definidas por dificuldade")
                .build(), event -> {
            Player p = (Player) event.getWhoClicked();
            saveQuest(ctx, p);
        });

        return true;
    }

    private void reopenMenu(@NotNull Player player, @NotNull PlayerMenuContext ctx) {
        Map<String, Object> data = Map.of(
                KEY_TYPE, ctx.getOrDefault(KEY_TYPE, QuestType.BLOCK_BREAK),
                KEY_FILTER, ctx.getOrDefault(KEY_FILTER, ""),
                KEY_DISPLAY, ctx.getOrDefault(KEY_DISPLAY, "Nova Quest"));
        Tasks.runSync(() -> MenuUtil.openMenu(player, QuestCreateMenu.class, new java.util.HashMap<>(data)));
    }

    private void saveQuest(@NotNull PlayerMenuContext ctx, @NotNull Player player) {
        QuestType type = ctx.getOrDefault(KEY_TYPE, QuestType.BLOCK_BREAK);
        String filter = ctx.getOrDefault(KEY_FILTER, "");
        String display = ctx.getOrDefault(KEY_DISPLAY, "Nova Quest");

        if (display.isEmpty()) {
            StringUtils.send(player, "<red>Defina um nome para a quest!");
            ERROR_SOUND.play(player);
            return;
        }

        String id = display.toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");

        if (id.isEmpty()) {
            id = "quest_" + System.currentTimeMillis();
        }

        if (plugin.getQuestManager().getTemplate(id) != null) {
            id = id + "_" + System.currentTimeMillis() % 10000;
        }

        QuestTemplate template = new QuestTemplate();
        template.setId(id);
        template.setDisplayName(display);
        template.setType(type);
        template.setTargetFilter(filter.isEmpty() ? null : filter.toUpperCase());

        final String finalId = id;
        Thread.startVirtualThread(() -> {
            plugin.getQuestManager().createTemplate(template);
            StringUtils.send(player, "<green>Quest <white>" + finalId + " <green>criada com sucesso!");
            SUCCESS_SOUND.play(player);
        });

        player.closeInventory();
    }
}

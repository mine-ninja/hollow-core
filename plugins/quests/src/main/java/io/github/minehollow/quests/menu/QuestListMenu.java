package io.github.minehollow.quests.menu;

import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.quests.QuestsPlugin;
import io.github.minehollow.quests.player.ActiveQuest;
import io.github.minehollow.quests.quest.QuestManager;
import io.github.minehollow.quests.quest.QuestTemplate;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
public class QuestListMenu extends SimpleMenu {
    private static final PredefinedSound CLAIM_SOUND = new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.0F);
    private static final PredefinedSound REROLL_SOUND = new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.2F);
    private static final PredefinedSound ERROR_SOUND = new PredefinedSound(Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);

    private final QuestsPlugin plugin;

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        final Player player = ctx.getPlayer();
        final var questManager = plugin.getQuestManager();
        final var data = plugin.getPlayerQuestService().getCachedData(player.getUniqueId());

        if (data == null) {
            StringUtils.send(player, "<red>Erro ao carregar dados. Tente novamente.");
            return false;
        }

        openHandler.setTitle(StringUtils.text("Quests Diárias"));
        openHandler.setRows(5);
        openHandler.setLayout(
                "    H    ",
                "         ",
                " QQQQQQQ ",
                " QQQQQQQ ",
                "    I    ");

        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));

        boolean premium = player.hasPermission("quests.premium");
        int claimLimit = questManager.getClaimLimit(player);
        int rerollLimit = questManager.getRerollLimit(player);
        int remainingClaims = Math.max(0, claimLimit - data.getClaimedToday());
        int remainingRerolls = Math.max(0, rerollLimit - data.getRerolledToday());

        ctx.setItem('H', ItemBuilder.skull(player)
                .name("<gradient:#FFD700:#FFA500><bold>QUESTS DIÁRIAS</bold></gradient>")
                .addLore(
                        " <dark_gray>Complete quests e ganhe recompensas!",
                        "",
                        " <white>Resgates: <green><bold>" + remainingClaims + "/" + claimLimit + "</bold>",
                        " <white>Rerolls: <green><bold>" + remainingRerolls + "/" + rerollLimit + "</bold>",
                        "",
                        premium
                                ? " <gradient:#FFD700:#FFA500>★ Premium Ativo</gradient>"
                                : " <gray>★ Sem Premium")
                .build());

        ctx.setItem('I', ItemBuilder.of(Material.BOOK)
                .name("<yellow>Como Funciona?")
                .addLore(
                        " <gray>Complete os objetivos das quests",
                        " <gray>e clique nelas para resgatar!",
                        "",
                        " <green>Clique esquerdo <gray>= Resgatar",
                        " <red>Clique direito <gray>= Reroll")
                .build());

        List<ActiveQuest> quests = data.getActiveQuests();
        int[] questSlots = openHandler.getLayout().get('Q');

        for (int i = 0; i < questSlots.length; i++) {
            if (i >= quests.size()) {
                ctx.setItem(questSlots[i], new ItemStack(Material.AIR));
                continue;
            }

            final int questIndex = i;
            final ActiveQuest quest = quests.get(i);
            final QuestTemplate template = questManager.getTemplate(quest.getTemplateId());

            if (template == null) {
                ctx.setItem(questSlots[i], ItemBuilder.of(Material.BARRIER)
                        .name("<red>Quest inválida")
                        .addLore(" <dark_gray>Template não encontrado.")
                        .build());
                continue;
            }

            ctx.setItem(questSlots[i],
                    p -> renderQuestIcon(p, quest, template, questManager),
                    event -> handleQuestClick(event, ctx, questIndex, quest, template));
        }

        return true;
    }

    private @NotNull ItemStack renderQuestIcon(
            @NotNull Player player,
            @NotNull ActiveQuest quest,
            @NotNull QuestTemplate template,
            @NotNull QuestManager questManager) {
        int effectiveProgress = questManager.getEffectiveProgress(quest, template);
        boolean completed = effectiveProgress >= quest.getRequiredAmount();
        int displayProgress = Math.min(effectiveProgress, quest.getRequiredAmount());

        if (quest.isClaimed()) {
            return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name("<gray><strikethrough>" + template.getDisplayName() + "</strikethrough>")
                    .addLore(
                            "",
                            " <gray>Já resgatada")
                    .build();
        }

        Material icon = completed ? Material.LIME_STAINED_GLASS_PANE : template.getType().getIcon();
        String color = completed ? "<green>" : "<yellow>";
        String progressBar = generateProgressBar(displayProgress, quest.getRequiredAmount());

        ItemBuilder builder = ItemBuilder.of(icon)
                .name(color + template.getDisplayName())
                .addLore(
                        "",
                        " <white>Objetivo: <gray>" + questManager.formatObjective(quest),
                        " <white>Progresso: " + progressBar + " <gray>" + displayProgress + "/"
                                + quest.getRequiredAmount(),
                        "");

        if (completed) {
            builder.glow();
            builder.addLore(" <green><bold>CLIQUE ESQUERDO PARA RESGATAR");
        } else {
            builder.addLore(" <yellow>Em progresso...");
        }
        builder.addLore(" <red>Clique direito para reroll");

        return builder.build();
    }

    private void handleQuestClick(
            @NotNull InventoryClickEvent event,
            @NotNull PlayerMenuContext ctx,
            int questIndex,
            @NotNull ActiveQuest quest,
            @NotNull QuestTemplate template) {
        Player player = (Player) event.getWhoClicked();
        var questManager = plugin.getQuestManager();

        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (quest.isClaimed()) {
                StringUtils.send(player, "<red>Essa quest já foi resgatada!");
                ERROR_SOUND.play(player);
                return;
            }

            boolean success = questManager.rerollQuest(player, questIndex);
            if (success) {
                StringUtils.send(player, "<green>Quest trocada com sucesso!");
                REROLL_SOUND.play(player);
                ctx.openMenu(QuestListMenu.class);
            } else {
                StringUtils.send(player, "<red>Você atingiu o limite de rerolls hoje!");
                ERROR_SOUND.play(player);
            }
            return;
        }

        if (quest.isClaimed()) {
            StringUtils.send(player, "<red>Você já resgatou essa quest!");
            ERROR_SOUND.play(player);
            return;
        }

        int effectiveProgress = questManager.getEffectiveProgress(quest, template);
        if (effectiveProgress < quest.getRequiredAmount()) {
            StringUtils.send(player, "<red>Você ainda não completou essa quest!");
            ERROR_SOUND.play(player);
            return;
        }

        boolean success = questManager.claimQuest(player, questIndex);
        if (success) {
            StringUtils.send(player, "<green><bold>SUCESSO!</bold> Recompensas da quest resgatadas!");
            CLAIM_SOUND.play(player);
            ctx.openMenu(QuestListMenu.class);
        } else {
            StringUtils.send(player, "<red>Você atingiu o limite de resgates hoje!");
            ERROR_SOUND.play(player);
        }
    }

    private String generateProgressBar(int current, int max) {
        int bars = 10;
        int filled = max > 0 ? Math.min((int) ((double) current / max * bars), bars) : 0;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("<green>■");
            } else {
                sb.append("<gray>■");
            }
        }
        return sb.toString();
    }
}

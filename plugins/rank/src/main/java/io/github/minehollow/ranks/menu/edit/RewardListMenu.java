package io.github.minehollow.ranks.menu.edit;

import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.reward.RankReward;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RewardListMenu extends SimpleMenu {
    private final RanksPlugin plugin;
    private static final int REWARDS_PER_PAGE = 45;

    public RewardListMenu(RanksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Player player = ctx.getPlayer();

        if (!player.hasPermission("ranks.admin")) {
            player.sendMessage(StringUtils.text("<red>Você não tem permissão para gerenciar recompensas!"));
            return false;
        }

        int page = ctx.getOrDefault("page", 0);

        config.setTitle(StringUtils.text("Recompensas de Rank - Página " + (page + 1)));
        config.setRows(6);
        config.setLayout(
          "RRRRRRRRR",
          "RRRRRRRRR",
          "RRRRRRRRR",
          "RRRRRRRRR",
          "RRRRRRRRR",
          "-P-N-<-> "
        );

        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        // Coletar todas as recompensas
        List<RankReward> allRewards = getAllRewards();
        int totalPages = (int) Math.ceil((double) allRewards.size() / REWARDS_PER_PAGE);

        // Separador
        ctx.setItem('-', p -> createSeparator(), e -> e.setCancelled(true));

        // Página anterior
        ctx.setItem('<', p -> createPreviousButton(page, totalPages), e -> {
            e.setCancelled(true);
            if (page > 0) {
                ctx.put("page", page - 1);
                ctx.openMenu(RewardListMenu.class, true);
            }
        });

        // Próxima página
        ctx.setItem('>', p -> createNextButton(page, totalPages), e -> {
            e.setCancelled(true);
            if (page < totalPages - 1) {
                ctx.put("page", page + 1);
                ctx.openMenu(RewardListMenu.class, true);
            }
        });

        // Informações da página
        ctx.setItem('P', p -> createPageInfo(page, totalPages, allRewards.size()), e -> e.setCancelled(true));

        // Nova recompensa
        ctx.setItem('N', p -> createNewRewardButton(), e -> {
            e.setCancelled(true);
            openNewRewardInput(ctx);
        });

        // Recompensas
        int[] rewardSlots = ctx.getMenuConfig().getLayout().get('R');
        int startIndex = page * REWARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + REWARDS_PER_PAGE, allRewards.size());

        for (int i = startIndex; i < endIndex; i++) {
            final RankReward reward = allRewards.get(i);
            final int slotIndex = i - startIndex;

            if (slotIndex < rewardSlots.length) {
                ctx.setItem(rewardSlots[slotIndex], createRewardItem(reward));
                ctx.put("reward_slot_" + rewardSlots[slotIndex], reward);
            }
        }

        return true;
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        RankReward reward = ctx.get("reward_slot_" + slot);

        if (reward != null) {
            // Abrir menu de edição
            Player player = ctx.getPlayer();
            player.closeInventory();

            // CORRIGIDO: Usar Tasks.runSync e passar a recompensa corretamente
            Tasks.runSync(() -> {
                ctx.openMenu(RewardEditMenu.class, Map.of("reward", reward));
            });
        }
    }

    private List<RankReward> getAllRewards() {
        List<RankReward> rewards = new ArrayList<>();

        // Percorrer todos os ranks e coletar recompensas únicas
        for (int rank = 1; rank <= RanksPlugin.MAX_LEVEL; rank++) {
            List<RankReward> rankRewards = plugin.getRankRewardManager().getRewardsForRank(rank);
            for (RankReward reward : rankRewards) {
                // Adicionar apenas se não estiver na lista (evitar duplicatas)
                if (rewards.stream().noneMatch(r -> r.id().equals(reward.id()))) {
                    rewards.add(reward);
                }
            }
        }

        return rewards;
    }

    private void openNewRewardInput(PlayerMenuContext ctx) {
        Player player = ctx.getPlayer();
        player.closeInventory();

        ChatInput.waitInput(player, input -> {
            if (!input.equalsIgnoreCase("cancelar") && !input.isBlank()) {
                // Sanitizar o ID antes de passar
                String rewardId = input.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");

                Tasks.runSync(() -> {
                    // Passar o ID correto via Map.of
                    ctx.openMenu(RewardEditMenu.class, Map.of("rewardId", rewardId));
                });
            } else {
                player.sendMessage(StringUtils.text("<red>Criação cancelada."));
                Tasks.runSync(() -> ctx.openMenu(RewardListMenu.class, true));
            }
        }, 30000L, "<green>Digite o ID da nova recompensa.\n<gray>Use apenas letras, números, _ e -\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private ItemStack createRewardItem(RankReward reward) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>ID: <white>" + reward.id());
        lore.add("<gray>Intervalo: <yellow>" + reward.range());

        if (reward.everyXLevels() > 0) {
            lore.add("<gray>Frequência: <yellow>A cada " + reward.everyXLevels() + " níveis");
        }

        if (reward.permissionToReceive() != null) {
            lore.add("<gray>Permissão: <yellow>" + reward.permissionToReceive());
        }

        lore.add("");
        lore.add("<gray>Comandos: <white>" + reward.commandsToExecute().size());
        lore.add("<gray>Itens: <white>" + reward.itemsToGive().size());
        lore.add("");
        lore.add("<dark_gray>Clique para editar");

        Material icon = Material.EMERALD;
        if (!reward.itemsToGive().isEmpty()) {
            icon = reward.itemsToGive().get(0).getType();
        }

        return ItemBuilder.of(icon)
          .name("<green>" + reward.displayName())
          .lore(lore)
          .build();
    }

    private ItemStack createSeparator() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
          .name(" ")
          .build();
    }

    private ItemStack createPageInfo(int currentPage, int totalPages, int totalRewards) {
        return ItemBuilder.of(Material.PAPER)
          .name("<white>Informações")
          .lore(
            "<gray>Página: <yellow>" + (currentPage + 1) + "<gray>/<yellow>" + Math.max(1, totalPages),
            "<gray>Total de recompensas: <yellow>" + totalRewards
          )
          .build();
    }

    private ItemStack createPreviousButton(int currentPage, int totalPages) {
        boolean canGoPrevious = currentPage > 0;

        return ItemBuilder.of(canGoPrevious ? Material.ARROW : Material.GRAY_DYE)
          .name(canGoPrevious ? "<yellow>← Página Anterior" : "<gray>Primeira Página")
          .build();
    }

    private ItemStack createNextButton(int currentPage, int totalPages) {
        boolean canGoNext = currentPage < totalPages - 1;

        return ItemBuilder.of(canGoNext ? Material.ARROW : Material.GRAY_DYE)
          .name(canGoNext ? "<yellow>Próxima Página →" : "<gray>Última Página")
          .build();
    }

    private ItemStack createNewRewardButton() {
        return ItemBuilder.of(Material.NETHER_STAR)
          .name("<green>✦ Nova Recompensa")
          .lore(
            "",
            "<dark_gray>Clique para criar"
          )
          .glow()
          .build();
    }
}
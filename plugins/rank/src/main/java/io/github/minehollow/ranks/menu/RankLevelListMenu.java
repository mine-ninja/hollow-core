package io.github.minehollow.ranks.menu;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationContext;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationMenu;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.ProgressBarGenerator;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.reward.RankReward;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class RankLevelListMenu extends DynamicPaginationMenu<Integer> {

    private static final ItemStack PREVIOUS_PAGE_ICON = ItemBuilder.of(Material.ARROW).name("<yellow>Página Anterior").build();
    private static final ItemStack NEXT_PAGE_ICON = ItemBuilder.of(Material.ARROW).name("<yellow>Próxima Página").build();
    private static final PredefinedSound SUCCESS_SOUND = new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.0F);

    private static final IntArrayList LEVELS = IntArrayList.wrap(IntStream.rangeClosed(1, 99).toArray());

    private final RanksPlugin plugin;

    @Override
    public boolean onPreOpen(@NotNull DynamicPaginationContext<Integer> ctx, @NotNull MenuConfig openHandler) {
        final var player = ctx.getPlayer();
        final var uuid = player.getUniqueId();
        final var playerProgress = plugin.getPlayerRankProgressService().getCachedProgress(uuid);

        final var wallet = BukkitPlatform.getInstance().getWalletService().getOrLoadWallet(uuid);
        if (playerProgress == null || wallet == null) {
            player.closeInventory();
            StringUtils.send(player, "<red>Erro ao carregar dados (Progresso/Carteira). Tente novamente.");
            return false;
        }

        openHandler.setTitle(StringUtils.text("Níveis de Rank"));
        openHandler.setRows(6);
        openHandler.setLayout(
          "    H    ",
          "         ",
          "  LLLLL  ",
          "P LLLLL N",
          "  LLLLL  ",
          "         "
        );

        Tasks.runAsync(() -> {
            ctx.setPagination('L', LEVELS, this::generateRankLevelIcon, (level, event) -> {
                giveRewards(level, event);
                ctx.update();
            });

            // --- Cabeçalho (Header) Otimizado ---
            final int currentRank = playerProgress.getCurrentRank();
            final double coins = wallet.getCurrencyAmount("rankup_coins").doubleValue();
            final double needed = plugin.getMoneyCostForRank(currentRank + 1);
            final double progressPercent = Math.clamp((coins / needed) * 100, 0, 100);

            final long unclaimedCount = LEVELS.stream()
              .filter(lvl -> lvl <= currentRank && !playerProgress.hasClaimedLevelReward(lvl))
              .count();

            final String progressBar = ProgressBarGenerator.generateStr(coins, needed, 10, '■', '■', "<gradient:#C77DFF:#9D4EDD>", "<gray>");

            ctx.setItem('H', ItemBuilder.skull(player)
              .name("<gradient:#C77DFF:#9D4EDD><bold>▎ SEU PERFIL</bold></gradient>")
              .addLore(
                " <dark_gray>Resumo da sua jornada</dark_gray>",
                "",
                " <gradient:#C77DFF:#9D4EDD>➲</gradient> <white>Nível <bold>" + currentRank + "</bold><yellow>✦",
                "   " + progressBar + " <gray>" + String.format("%.1f%%", progressPercent),
                "",
                " <#C77DFF>★</#C77DFF> <white>Prestígio <bold>" + playerProgress.getPrestigeLevel() + "</bold>",
                "",
                " <gradient:#FDB924:#FAD02C>🎁 " + unclaimedCount + " recompensas prontas!</gradient>"
              ).build());

            ctx.update();
        });

        ctx.setNextButton('N', NEXT_PAGE_ICON);
        ctx.setPreviousButton('P', PREVIOUS_PAGE_ICON);
        return true;
    }

    private @NotNull ItemStack generateRankLevelIcon(@NotNull Player player, int level) {
        final var progress = plugin.getPlayerRankProgressService().getCachedProgress(player.getUniqueId());
        if (progress == null) return new ItemStack(Material.AIR);

        final int currentRank = progress.getCurrentRank();
        final boolean hasClaimed = progress.hasClaimedLevelReward(level);
        final boolean isUnlocked = level <= currentRank;

        // Definição de Estado
        Material material;
        String color;
        String status;
        boolean canClaimNow = isUnlocked && !hasClaimed;

        if (hasClaimed) {
            material = Material.BLACK_STAINED_GLASS_PANE;
            color = "<#808080>";
            status = "<gray>✔ Recompensas resgatadas";
        } else if (level == currentRank) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
            color = "<#FAD02C>";
            status = "<yellow>➲ Nível Atual";
        } else if (isUnlocked) {
            material = Material.LIME_STAINED_GLASS_PANE;
            color = "<#40ff00>";
            status = "<green>✦ Nível Alcançado";
        } else {
            material = Material.RED_STAINED_GLASS_PANE;
            color = "<#FF4B2B>";
            status = "<dark_gray>✕ Bloqueado";
        }

        ItemBuilder builder = ItemBuilder.of(material)
          .name(color + "<bold>RANK " + level)
          .amount(Math.clamp(level, 1, 64)) // Minecraft padrão limita ícones a 64
          .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
          .addLore(" " + status);

        // Adiciona custo apenas se não foi alcançado
        if (!isUnlocked) {
            double cost = plugin.getMoneyCostForRank(level);
            builder.addLore(" <gray>Investimento: <green>$" + String.format("%.0f", cost));
        }

        builder.addLore("", "<gray>Prêmios de Evolução:");

        List<RankReward> rewards = plugin.getRankRewardManager().getRewardsForRank(level);
        if (rewards.isEmpty()) {
            builder.addLore(" <dark_gray>• Sem prêmios");
        } else {
            rewards.forEach(r -> builder.addLore(" <white>• " + r.displayName()));
        }

        // --- Lógica de Brilho (Glow) e Chamada de Ação ---
        if (canClaimNow) {
            builder.glow();
            builder.addLore("", " <gradient:#2ECC71:#27AE60><bold>CLICK PARA RESGATAR</bold></gradient>");
        } else if (!isUnlocked) {
            builder.addLore("", " <red>Bloqueado: <gray>Alcance este nível primeiro");
        }

        return builder.build();
    }

    private void giveRewards(int level, @NotNull InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final var progress = plugin.getPlayerRankProgressService().getCachedProgress(player.getUniqueId());

        if (progress == null || progress.getCurrentRank() < level) return;
        if (progress.hasClaimedLevelReward(level)) return;

        if (!plugin.getRankRewardManager().canReceiveRewards(player, level)) {
            StringUtils.send(player, "<red>Inventário cheio!");
            return;
        }

        try {
            plugin.getRankRewardManager().giveRewardsToPlayer(player, level);
            progress.markLevelRewardClaimed(level);
            plugin.getPlayerRankProgressService().updatePlayerProgress(progress, true);
            SUCCESS_SOUND.play(player);
        } catch (Exception e) {
            log.error("Erro ao entregar prêmio nível {} para {}", level, player.getName(), e);
        }
    }
}
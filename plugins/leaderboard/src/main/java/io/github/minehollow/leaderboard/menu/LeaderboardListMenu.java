package io.github.minehollow.leaderboard.menu;

import io.github.minehollow.leaderboard.LeaderboardManager;
import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.sdk.stats.StatEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Main menu listing all registered leaderboards.
 * Clicking a leaderboard opens a detailed chat view with the player's rank.
 */
public class LeaderboardListMenu extends SimpleMenu {

    private final LeaderboardManager manager;

    public LeaderboardListMenu(@NotNull LeaderboardManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        var leaderboards = new ArrayList<>(manager.getAll());

        int size = leaderboards.size();
        int rows = Math.min(6, Math.max(1, (size / 7) + 2));

        openHandler.setTitle(StringUtils.text("<black>Rankings"));
        openHandler.setRows(rows);

        Player player = ctx.getPlayer();

        for (int i = 0; i < size && i < (rows * 9 - 2); i++) {
            LeaderboardConfig config = leaderboards.get(i);
            List<StatEntry> cached = manager.getCachedEntries(config.id());

            int slot = computeSlot(i, rows);
            ctx.setItem(slot,
                p -> buildIcon(config, cached, p),
                event -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                    Tasks.runAsync(() -> {
                        List<Component> lines = manager.buildChatLines(config.id(), player);
                        Tasks.runSync(() -> lines.forEach(player::sendMessage));
                    });
                }
            );
        }

        if (leaderboards.isEmpty()) {
            int center = (rows * 9) / 2;
            ctx.setItem(center, ItemBuilder.of(Material.BARRIER)
                .name("<red>Nenhum ranking encontrado")
                .addLore(" ", "<gray>Nenhum ranking foi registrado ainda.", " ")
                .build()
            );
        }

        return true;
    }

    private org.bukkit.inventory.ItemStack buildIcon(@NotNull LeaderboardConfig config,
                                                      @NotNull List<StatEntry> cached,
                                                      @NotNull Player player) {
        Material icon = config.icon();
        if (icon == null || icon == Material.AIR) icon = Material.DIAMOND_SWORD;

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("<gray>Stat: <white>" + config.statKey());
        lore.add("<gray>Período: <white>" + translatePeriod(config.period().name()));
        lore.add(" ");

        int show = Math.min(cached.size(), 5);
        if (show > 0) {
            lore.add("<yellow>Top " + show + ":");
            for (int j = 0; j < show; j++) {
                StatEntry entry = cached.get(j);
                String name = resolveName(entry.playerId());
                lore.add("<gray> " + (j + 1) + ". <white>" + name + " <gray>- <aqua>" + formatValue(entry.value()));
            }
        } else {
            lore.add("<dark_gray>Sem dados ainda.");
        }

        lore.add(" ");
        lore.add("<yellow>Clique para ver o ranking completo");
        lore.add(" ");

        return ItemBuilder.of(icon)
            .name(config.displayName())
            .addLore(lore.toArray(String[]::new))
            .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
            .build();
    }

    /**
     * Distributes items centered in the inventory rows, skipping the border.
     */
    private int computeSlot(int index, int rows) {
        int itemsPerRow = 7;
        int row = index / itemsPerRow;
        int col = index % itemsPerRow;
        return (row * 9) + col + 1;
    }

    private @NotNull String resolveName(@NotNull java.util.UUID playerId) {
        Player online = org.bukkit.Bukkit.getPlayer(playerId);
        if (online != null) return online.getName();
        org.bukkit.OfflinePlayer offline = org.bukkit.Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : "???";
    }

    private @NotNull String formatValue(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }

    private @NotNull String translatePeriod(@NotNull String period) {
        return switch (period) {
            case "DAILY" -> "Diário";
            case "WEEKLY" -> "Semanal";
            case "MONTHLY" -> "Mensal";
            case "ALLTIME" -> "Geral";
            default -> period;
        };
    }
}


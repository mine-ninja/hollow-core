package io.github.minehollow.ranks.listener;

import io.github.minehollow.ranks.RankRewardItemHolder;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.reward.RankReward;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public record RankRewardListener(RanksPlugin plugin) implements Listener {

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RankRewardItemHolder holder)) return;

        RankReward oldReward = holder.getReward();
        var items = Arrays.stream(event.getInventory().getContents())
          .filter(Objects::nonNull)
          .filter(it -> it.getType() != Material.AIR)
          .toList();

        RankReward newReward = new RankReward(
          oldReward.id(), oldReward.range(), oldReward.everyXLevels(),
          oldReward.permissionToReceive(), oldReward.displayName(),
          oldReward.commandsToExecute(), items
        );

        plugin.getRankRewardManager().saveReward(newReward);
    }
}
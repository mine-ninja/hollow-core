package io.github.minehollow.ranks;

import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.ranks.reward.RankReward;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class RankRewardItemHolder implements InventoryHolder {

    private final RankReward reward;
    private final Inventory inventory;

    public RankRewardItemHolder(@NotNull RankReward reward) {
        this.reward = reward;
        this.inventory = Bukkit.createInventory(this, 9 * 6, StringUtils.formatString("Feche para salvar!"));
    }

    @NotNull
    public RankReward getReward() {
        return reward;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}


package io.github.minehollow.ranks.reward;

import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.util.ExpressionProcessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class RankRewardManager {

    private final RanksPlugin plugin;
    private final Int2ObjectMap<List<RankReward>> rewardsByRank = new Int2ObjectArrayMap<>();

    public void reloadRewards() {
        this.rewardsByRank.clear();
        this.loadRewards();
    }

    public void saveReward(@NotNull RankReward reward) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rank-rewards");
        if (section == null) {
            section = plugin.getConfig().createSection("rank-rewards");
        }

        reward.writeToSection(section);
        plugin.saveConfig();

        this.reloadRewards();
    }

    public void deleteReward(@NotNull String id) {
        plugin.getConfig().set("rank-rewards." + id, null);
        plugin.saveConfig();
        this.reloadRewards();
    }

    public void loadRewards() {
        final var config = plugin.getConfig();
        final var rewardSection = config.getConfigurationSection("rank-rewards");
        if (rewardSection == null) {
            plugin.getLogger().warning("No rank rewards found in config!");
            return;
        }

        rewardSection.getKeys(false)
          .stream()
          .map(rewardSection::getConfigurationSection)
          .filter(Objects::nonNull)
          .map(RankReward::readFromSection)
          .forEach(this::registerRankReward);


        Bukkit.getConsoleSender().sendMessage(
          "§aLoaded §e" + countTotalRewards() + "§a rank rewards for §e" + rewardsByRank.size() + "§a ranks."
        );
    }

    // Local: RankRewardManager.java

    public boolean canReceiveRewards(Player player, int rank) {
        final var rewards = getRewardsForRank(rank);
        if (rewards.isEmpty()) return true;

        return rewards.stream().allMatch(reward -> reward.canBeGivenToPlayer(player));
    }

    public void giveRewardsToPlayer(@NotNull Player player, int rank) {
        final var rewards = getRewardsForRank(rank);
        rewards.stream()
          .filter(reward -> reward.canBeGivenToPlayer(player))
          .forEach(reward -> {

              reward.commandsToExecute().forEach(command -> {
                  final var processed = ExpressionProcessor.process(
                    command.replace("%player%", player.getName()),
                    rank
                  );

                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
              });

              reward.itemsToGive().forEach(itemStack -> PlayerUtil.addItem(player, itemStack, itemStack.getAmount()));
          });


    }

    public void registerRankReward(@NotNull RankReward reward) {
        final var levelRange = reward.range();
        final int everyXLevels = reward.everyXLevels();

        for (int rank = levelRange.min(); rank <= levelRange.max(); rank++) {
            if (everyXLevels > 0 && rank % everyXLevels != 0) {
                continue;
            }
            addRewardToLevel(rank, reward);
        }
    }

    public void addRewardToLevel(int rank, RankReward reward) {
        rewardsByRank.computeIfAbsent(rank, r -> new ArrayList<>()).add(reward);
    }

    public List<RankReward> getRewardsForRank(int rank) {
        return rewardsByRank.getOrDefault(rank, Collections.emptyList());
    }

    public long countTotalRewards() {
        return rewardsByRank.values().stream().mapToLong(List::size).sum();
    }
}
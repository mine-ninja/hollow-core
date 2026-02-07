package io.github.minehollow.ranks;

import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.ranks.command.RankCommand;
import io.github.minehollow.ranks.command.RankRewardEditCommand;
import io.github.minehollow.ranks.listener.LevelUpListener;
import io.github.minehollow.ranks.listener.RankRewardListener;
import io.github.minehollow.ranks.menu.RankLevelListMenu;
import io.github.minehollow.ranks.menu.edit.RewardEditMenu;
import io.github.minehollow.ranks.menu.edit.RewardListMenu;
import io.github.minehollow.ranks.progress.PlayerRankProgressService;
import io.github.minehollow.ranks.reward.RankRewardManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import lombok.Getter;
import net.objecthunter.exp4j.ExpressionBuilder;

@Getter
public class RanksPlugin extends SimplePlugin {

    public static final int MAX_LEVEL = 99;

    private PlayerRankProgressService playerRankProgressService;
    private RankRewardManager rankRewardManager;

    private final Int2DoubleMap cachedMoneyCosts = new Int2DoubleArrayMap();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.playerRankProgressService = new PlayerRankProgressService(this);

        this.rankRewardManager = new RankRewardManager(this);
        this.rankRewardManager.loadRewards();

        registerCommands("ranks", new RankRewardEditCommand(this), new RankCommand(this));
        registerListeners(new RankRewardListener(this), new LevelUpListener(this));

        MenuUtil.registerMenus(new RankLevelListMenu(this), new RewardEditMenu(this), new RewardListMenu(this));

        this.computeMoneyCostRankValues();
    }

    public double getMoneyCostForRank(int rank) {
        return cachedMoneyCosts.getOrDefault(rank, -1D);
    }

    public void computeMoneyCostRankValues() {
        this.invalidateMoneyCostCache();

        final var formula = this.getConfig().getString("level-up-formula");
        if (formula == null) {
            throw new IllegalStateException("Money cost formula is not defined in config!");
        }

        for (int i = 0; i < MAX_LEVEL; i++) {
            final var computedMoneyCost = new ExpressionBuilder(formula)
              .variable("level")
              .build()
              .setVariable("level", i)
              .evaluate();

            this.cachedMoneyCosts.put(i, (long) computedMoneyCost);
        }
    }


    public void invalidateMoneyCostCache() {
        this.cachedMoneyCosts.clear();
    }
}
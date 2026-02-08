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
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.ExpressionBuilder;

@Slf4j
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
        if (rank > MAX_LEVEL) {
            return 0D;
        }
        return cachedMoneyCosts.getOrDefault(rank, -1D);
    }

    public void computeMoneyCostRankValues() {
        this.invalidateMoneyCostCache();

        final String formula = this.getConfig().getString("level-up-formula");
        if (formula == null || formula.isEmpty()) {
            log.error("A fórmula de custo (level-up-formula) não foi definida na config!");
            return;
        }

        log.info("Calculando custos de rank até o nível {}...", MAX_LEVEL);

        for (int i = 1; i <= MAX_LEVEL; i++) {
            try {
                final double computedMoneyCost = new ExpressionBuilder(formula)
                  .variable("level")
                  .build()
                  .setVariable("level", i)
                  .evaluate();

                this.cachedMoneyCosts.put(i, (double) (long) computedMoneyCost);
            } catch (Exception e) {
                log.error("Erro ao calcular a fórmula para o nível {}: {}", i, e.getMessage());
                this.cachedMoneyCosts.put(i, -1D);
            }
        }

        log.info("Cache de custos de rank finalizado com {} entradas.", cachedMoneyCosts.size());
    }

    public void invalidateMoneyCostCache() {
        this.cachedMoneyCosts.clear();
    }
}
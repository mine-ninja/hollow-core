package io.github.minehollow.ranks.listener;

import io.github.minehollow.minecraft.event.wallet.PlayerWalletBalanceChangeEvent;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.event.PlayerRankUpEvent;
import io.github.minehollow.ranks.reward.RankReward;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Slf4j
@RequiredArgsConstructor
public class LevelUpListener implements Listener {

    private static final PredefinedSound LEVEL_UP_SOUND = new PredefinedSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

    private static final String[] RAW_RANKUP_MESSAGE = {
        " ",
        "  <gradient:#C77DFF:#9D4EDD><bold>✨ EVOLUÇÃO DE RANK!</bold></gradient> <white>  @oldRank  <gradient:#C77DFF:#9D4EDD>➔</gradient>  <bold>@newRank</bold></white>",
        "  <gray>Parabéns, sua jornada avançou.</gray>",
        " "
    };

    private final RanksPlugin plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    void handleBalanceChange(PlayerWalletBalanceChangeEvent event) {
        if (event.isCancelled() || !event.isIncrease()) {
            return;
        }

        final var playerProgress = plugin.getPlayerRankProgressService().getCachedProgress(event.getPlayerId());
        if (playerProgress == null) {
            log.warn("Received balance change event for player {} but their progress is not loaded yet. Ignoring the event.", event.getPlayerId());
            return;
        }

        final double requiredMoneyToLevelUp = plugin.getMoneyCostForRank(playerProgress.getCurrentRank() + 1);
        final double newBalance = event.getNewBalance().doubleValue();
        if (newBalance >= requiredMoneyToLevelUp) {
            final var rankupEvent = new PlayerRankUpEvent(event.getPlayerId(), playerProgress.getCurrentRank(), playerProgress.getCurrentRank() + 1);
            if (!rankupEvent.callEvent()) {
                return;
            }

            final int newRank = rankupEvent.getNewRank();
            playerProgress.setCurrentRank(newRank);
            plugin.getPlayerRankProgressService().updatePlayerProgress(playerProgress);

            final var localPlayer = Bukkit.getPlayer(event.getPlayerId());
            if (localPlayer == null) {
                return;
            }

            // Show rewards for the rank just completed (oldRank) — same as the menu shows
            // on that rank's icon. canClaimLevelReward requires currentRank > level, so
            // rewards for oldRank become claimable exactly when you reach newRank.
            final List<RankReward> rewards = plugin.getRankRewardManager().getRewardsForRank(rankupEvent.getOldRank());
            System.out.println("Rank " + rankupEvent.getOldRank() + " rewards: " + rewards);

            // Build the final message lines
            final var messageLines = new ArrayList<>(List.of(RAW_RANKUP_MESSAGE));
            if (!rewards.isEmpty()) {
                messageLines.add("  <yellow>Recompensas disponíveis:</yellow>");
                for (final var reward : rewards) {
                    messageLines.add("  <gray>•</gray> <white>" + reward.displayName() + "</white>");
                }
                messageLines.add(" ");
                messageLines.add("  <white>Utilize <bold>/rank</bold> para coletar seus prêmios!</white>");
            } else {
                messageLines.add("  <gray>Sem recompensas para este rank, mas continue evoluindo para ganhar prêmios incríveis!</gray>");
            }
            messageLines.add(" ");

            LEVEL_UP_SOUND.play(localPlayer);
            for (final var line : messageLines) {
                localPlayer.sendMessage(StringUtils.formatString(line
                    .replace("@oldRank", String.valueOf(rankupEvent.getOldRank()))
                    .replace("@newRank", String.valueOf(rankupEvent.getNewRank()))));
            }
        }
    }
}

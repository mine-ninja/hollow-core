package io.github.minehollow.ranks.listener;

import io.github.minehollow.minecraft.event.wallet.PlayerWalletBalanceChangeEvent;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.event.PlayerRankUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Slf4j
@RequiredArgsConstructor
public class LevelUpListener implements Listener {

    private static final String[] RAW_RANKUP_MESSAGE = {
      " ",
      "  <gradient:#C77DFF:#9D4EDD><bold>✨ EVOLUÇÃO DE RANK!</bold></gradient> <white>  @oldRank  <gradient:#C77DFF:#9D4EDD>➔</gradient>  <bold>@newRank</bold></white>",
      "  <gray>Parabéns, sua jornada avançou:</gray>",
      " ",
      " ",
      "  <white>Utilize <bold>/rank</bold> para coletar seus prêmios!</white>",
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
            return; // this can happen if the player is not fully loaded yet, so we just ignore it and wait for the next balance change event
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

            for (final var line : RAW_RANKUP_MESSAGE) {
                final var formattedLine = line
                  .replace("@oldRank", String.valueOf(rankupEvent.getOldRank()))
                  .replace("@newRank", String.valueOf(rankupEvent.getNewRank()));

                localPlayer.sendMessage(StringUtils.formatString(formattedLine));
            }
        }
    }
}

package io.github.minehollow.quests.listener;

import io.github.minehollow.quests.quest.QuestManager;
import io.github.minehollow.quests.quest.QuestType;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

@RequiredArgsConstructor
public class QuestFishingListener implements Listener {
    private final QuestManager questManager;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        questManager.incrementProgress(
                event.getPlayer(),
                QuestType.FISHING,
                null,
                1);
    }
}

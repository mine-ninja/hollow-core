package io.github.minehollow.quests.listener;

import io.github.minehollow.quests.quest.QuestManager;
import io.github.minehollow.quests.quest.QuestType;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

@RequiredArgsConstructor
public class QuestMobKillListener implements Listener {
    private final QuestManager questManager;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        questManager.incrementProgress(
                killer,
                QuestType.MOB_KILL,
                event.getEntity().getType().name(),
                1);
    }
}

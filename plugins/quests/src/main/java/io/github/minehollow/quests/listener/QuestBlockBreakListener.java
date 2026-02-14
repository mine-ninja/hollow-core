package io.github.minehollow.quests.listener;

import io.github.minehollow.quests.quest.QuestManager;
import io.github.minehollow.quests.quest.QuestType;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@RequiredArgsConstructor
public class QuestBlockBreakListener implements Listener {
    private final QuestManager questManager;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        questManager.incrementProgress(
                event.getPlayer(),
                QuestType.BLOCK_BREAK,
                event.getBlock().getType().name(),
                1);
    }
}

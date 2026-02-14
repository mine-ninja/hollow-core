package io.github.minehollow.quests;

import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.quests.command.QuestsAdminCommand;
import io.github.minehollow.quests.command.QuestsCommand;
import io.github.minehollow.quests.listener.QuestBlockBreakListener;
import io.github.minehollow.quests.listener.QuestFishingListener;
import io.github.minehollow.quests.listener.QuestMobKillListener;
import io.github.minehollow.quests.menu.QuestCreateMenu;
import io.github.minehollow.quests.menu.QuestListMenu;
import io.github.minehollow.quests.player.PlayerQuestService;
import io.github.minehollow.quests.quest.QuestManager;
import lombok.Getter;

@Getter
public class QuestsPlugin extends SimplePlugin {
    private PlayerQuestService playerQuestService;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        playerQuestService = new PlayerQuestService(this);
        questManager = new QuestManager(this, playerQuestService);

        registerCommands("quests",
                new QuestsCommand(),
                new QuestsAdminCommand(this));

        registerListeners(
                new QuestBlockBreakListener(questManager),
                new QuestMobKillListener(questManager),
                new QuestFishingListener(questManager));

        MenuUtil.registerMenus(
                new QuestListMenu(this),
                new QuestCreateMenu(this));
    }
}

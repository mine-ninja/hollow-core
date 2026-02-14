package io.github.minehollow.quests.quest;

import io.github.minehollow.quests.QuestsPlugin;
import io.github.minehollow.quests.player.ActiveQuest;
import io.github.minehollow.quests.player.PlayerQuestData;
import io.github.minehollow.quests.player.PlayerQuestService;
import io.github.minehollow.sdk.util.data.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class QuestManager {
    private static final int FREE_QUEST_COUNT = 5;
    private static final int PREMIUM_QUEST_COUNT = 8;
    private static final int FREE_CLAIM_LIMIT = 5;
    private static final int PREMIUM_CLAIM_LIMIT = 8;
    private static final int FREE_REROLL_LIMIT = 1;
    private static final int PREMIUM_REROLL_LIMIT = 3;

    private final PlayerQuestService playerQuestService;
    private final MongoRepository<String, QuestTemplate> templateRepository;
    private final Map<String, QuestTemplate> templateCache = new ConcurrentHashMap<>();

    public QuestManager(@NotNull QuestsPlugin plugin, @NotNull PlayerQuestService playerQuestService) {
        this.playerQuestService = playerQuestService;
        this.templateRepository = new MongoRepository<>(QuestTemplate.class, "_id", "quest_templates");
        loadTemplates();
    }

    public void loadTemplates() {
        templateCache.clear();
        var templates = templateRepository.queryAll();
        for (var template : templates) {
            templateCache.put(template.getId(), template);
        }
        log.info("Loaded {} quest templates from database.", templateCache.size());
    }

    public void createTemplate(@NotNull QuestTemplate template) {
        templateRepository.save(template.getId(), template);
        templateCache.put(template.getId(), template);
        log.info("Created quest template: {}", template.getId());
    }

    public void deleteTemplate(@NotNull String id) {
        templateRepository.deleteById(id);
        templateCache.remove(id);
        log.info("Deleted quest template: {}", id);
    }

    @Nullable
    public QuestTemplate getTemplate(@NotNull String id) {
        return templateCache.get(id);
    }

    @NotNull
    public Collection<QuestTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templateCache.values());
    }

    public void assignDailyQuests(@NotNull PlayerQuestData data, int maxQuests) {
        var templates = new ArrayList<>(templateCache.values());
        if (templates.isEmpty()) {
            log.warn("No quest templates available. Cannot assign daily quests.");
            return;
        }

        Set<String> assigned = new HashSet<>();
        for (var aq : data.getActiveQuests()) {
            assigned.add(aq.getTemplateId());
        }
        templates.removeIf(t -> assigned.contains(t.getId()));

        int slotsToFill = maxQuests - data.getActiveQuests().size();
        if (slotsToFill <= 0 || templates.isEmpty())
            return;

        Collections.shuffle(templates, ThreadLocalRandom.current());
        int count = Math.min(slotsToFill, templates.size());

        for (int i = 0; i < count; i++) {
            QuestTemplate t = templates.get(i);
            QuestDifficulty difficulty = QuestDifficulty.randomWeighted();
            data.getActiveQuests().add(new ActiveQuest(t.getId(), difficulty, t.getType()));
        }
    }

    public void incrementProgress(@NotNull Player player, @NotNull QuestType type,
                                  @Nullable String filter, int amount) {
        var data = playerQuestService.getCachedData(player.getUniqueId());
        if (data == null)
            return;

        boolean changed = false;
        for (var quest : data.getActiveQuests()) {
            if (quest.isClaimed())
                continue;

            var template = getTemplate(quest.getTemplateId());
            if (template == null)
                continue;
            if (template.getType() != type)
                continue;

            if (template.getTargetFilter() != null && filter != null
                    && !template.getTargetFilter().equalsIgnoreCase(filter)) {
                continue;
            }

            quest.incrementProgress(amount);
            changed = true;
        }

        if (changed) {
            playerQuestService.save(data);
        }
    }

    public int getEffectiveProgress(@NotNull ActiveQuest quest, @NotNull QuestTemplate template) {
        if (template.getType() == QuestType.PLAY_TIME) {
            long elapsedMinutes = (System.currentTimeMillis() - quest.getAssignedAt()) / 60_000;
            return (int) elapsedMinutes;
        }
        return quest.getCurrentProgress();
    }

    public int getClaimLimit(@NotNull Player player) {
        return player.hasPermission("quests.premium") ? PREMIUM_CLAIM_LIMIT : FREE_CLAIM_LIMIT;
    }

    public int getRerollLimit(@NotNull Player player) {
        return player.hasPermission("quests.premium") ? PREMIUM_REROLL_LIMIT : FREE_REROLL_LIMIT;
    }

    public int getDailyQuestCount(@NotNull Player player) {
        return player.hasPermission("quests.premium") ? PREMIUM_QUEST_COUNT : FREE_QUEST_COUNT;
    }

    public boolean claimQuest(@NotNull Player player, int questIndex) {
        var data = playerQuestService.getCachedData(player.getUniqueId());
        if (data == null)
            return false;
        if (questIndex < 0 || questIndex >= data.getActiveQuests().size())
            return false;

        var quest = data.getActiveQuests().get(questIndex);
        if (quest.isClaimed())
            return false;

        var template = getTemplate(quest.getTemplateId());
        if (template == null)
            return false;

        int effectiveProgress = getEffectiveProgress(quest, template);
        if (effectiveProgress < quest.getRequiredAmount())
            return false;

        int claimLimit = getClaimLimit(player);
        if (data.getClaimedToday() >= claimLimit)
            return false;

        quest.setClaimed(true);
        data.setClaimedToday(data.getClaimedToday() + 1);

        for (String cmd : quest.getRewardCommands()) {
            String processed = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }

        playerQuestService.save(data);
        return true;
    }

    public boolean rerollQuest(@NotNull Player player, int questIndex) {
        var data = playerQuestService.getCachedData(player.getUniqueId());
        if (data == null)
            return false;
        if (questIndex < 0 || questIndex >= data.getActiveQuests().size())
            return false;

        var quest = data.getActiveQuests().get(questIndex);
        if (quest.isClaimed())
            return false;

        int rerollLimit = getRerollLimit(player);
        if (data.getRerolledToday() >= rerollLimit)
            return false;

        var templates = new ArrayList<>(templateCache.values());
        templates.removeIf(t -> t.getId().equals(quest.getTemplateId()));

        Set<String> assigned = new HashSet<>();
        for (var aq : data.getActiveQuests()) {
            assigned.add(aq.getTemplateId());
        }
        templates.removeIf(t -> assigned.contains(t.getId()));

        QuestTemplate newTemplate;
        if (templates.isEmpty()) {
            newTemplate = getTemplate(quest.getTemplateId());
            if (newTemplate == null)
                return false;
        } else {
            newTemplate = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        }

        QuestDifficulty newDifficulty = QuestDifficulty.randomWeighted();

        data.getActiveQuests().set(questIndex,
                new ActiveQuest(newTemplate.getId(), newDifficulty, newTemplate.getType()));
        data.setRerolledToday(data.getRerolledToday() + 1);

        playerQuestService.save(data);
        return true;
    }

    @NotNull
    public String formatObjective(@NotNull ActiveQuest quest) {
        var template = getTemplate(quest.getTemplateId());
        if (template == null)
            return "Quest desconhecida";

        int amount = quest.getRequiredAmount();
        boolean hasFilter = template.getTargetFilter() != null && !template.getTargetFilter().isEmpty();
        String target = hasFilter ? prettifyName(template.getTargetFilter()) : null;

        return switch (template.getType()) {
            case BLOCK_BREAK -> hasFilter
                    ? "Quebre " + amount + " " + target
                    : "Quebre " + amount + " blocos";
            case MOB_KILL -> hasFilter
                    ? "Mate " + amount + " " + target + "s"
                    : "Mate " + amount + " mobs";
            case FISHING -> "Pesque " + amount + " peixes";
            case PLAY_TIME -> "Fique online por " + amount + " minuto(s)";
        };
    }

    private String prettifyName(String raw) {
        if (raw == null || raw.isEmpty())
            return raw;
        String[] words = raw.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1));
        }
        return sb.toString();
    }
}

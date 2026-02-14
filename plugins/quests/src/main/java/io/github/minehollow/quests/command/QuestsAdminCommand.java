package io.github.minehollow.quests.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.quests.QuestsPlugin;
import io.github.minehollow.quests.menu.QuestCreateMenu;
import io.github.minehollow.quests.quest.QuestTemplate;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QuestsAdminCommand extends SimpleCommand {
    private final QuestsPlugin plugin;

    public QuestsAdminCommand(@NotNull QuestsPlugin plugin) {
        super("questadmin", "quests.admin");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            ctx.sendMessage(
                    "§e/questadmin create §7- Cria uma quest nova",
                    "§e/questadmin delete <id> §7- Deleta uma quest",
                    "§e/questadmin list §7- Lista todas as quests",
                    "§e/questadmin reload §7- Recarrega as quests do banco");
            return;
        }

        switch (subCommand.toLowerCase()) {
            case "create" -> handleCreate(ctx);
            case "delete" -> handleDelete(ctx);
            case "list" -> handleList(ctx);
            case "reload" -> handleReload(ctx);
            default ->
                    throw new CommandFailedException("§cSub-comando inválido. Use /questadmin para ver os comandos.");
        }
    }

    private void handleCreate(@NotNull CommandContext ctx) {
        Player player = ctx.getSenderAsPlayer();
        MenuUtil.openMenu(player, QuestCreateMenu.class);
    }

    private void handleDelete(@NotNull CommandContext ctx) {
        String id = ctx.getRawArgOrThrow(1, "§cUse: /questadmin delete <id>");

        var template = plugin.getQuestManager().getTemplate(id);
        if (template == null) {
            throw new CommandFailedException("§cQuest com id '" + id + "' não encontrada.");
        }

        Thread.startVirtualThread(() -> {
            plugin.getQuestManager().deleteTemplate(id);
            StringUtils.send(ctx.getSenderAsPlayer(), "<green>Quest <white>" + id + " <green>deletada com sucesso.");
        });
    }

    private void handleList(@NotNull CommandContext ctx) {
        var templates = plugin.getQuestManager().getAllTemplates();
        if (templates.isEmpty()) {
            ctx.sendMessage("§cNenhuma quest encontrada.");
            return;
        }

        ctx.sendMessage("§e§lQuests Disponíveis (" + templates.size() + "):");
        for (QuestTemplate t : templates) {
            ctx.sendMessage("§7 - §f" + t.getId() + " §7| " + t.getDisplayName()
                    + " §7| " + t.getType().getDisplayName()
                    + (t.getTargetFilter() != null ? " §7(" + t.getTargetFilter() + ")" : ""));
        }
    }

    private void handleReload(@NotNull CommandContext ctx) {
        Thread.startVirtualThread(() -> {
            plugin.getQuestManager().loadTemplates();

            for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
                var data = plugin.getPlayerQuestService().getCachedData(player.getUniqueId());
                if (data != null) {
                    plugin.getQuestManager().assignDailyQuests(data,
                            plugin.getQuestManager().getDailyQuestCount(player));
                    plugin.getPlayerQuestService().saveSync(data);
                }
            }

            StringUtils.send(ctx.getSenderAsPlayer(), "<green>Quests recarregadas e atribuídas aos jogadores online.");
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            return filterStartingWith(
                    List.of("create", "delete", "list", "reload"),
                    ctx.getRawArgOrNull(0));
        }

        if (ctx.getArgs().length == 2 && "delete".equalsIgnoreCase(ctx.getRawArgOrNull(0))) {
            return filterStartingWith(
                    plugin.getQuestManager().getAllTemplates().stream()
                            .map(QuestTemplate::getId)
                            .toList(),
                    ctx.getRawArgOrNull(1));
        }

        return NONE_ARGS;
    }
}

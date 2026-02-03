package io.github.minehollow.kits.command.subcommand;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.menu.KitCategoryEditorMenu;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CategorySubCommand extends SimpleSubCommand {
    private final KitService kitService;
    private final BukkitPlatform platform;

    public CategorySubCommand(KitService kitService, BukkitPlatform platform) {
        super("category");
        this.kitService = kitService;
        this.platform = platform;
        this.permission = "kit.admin";
        this.playersOnly = false;
        this.aliases = List.of("cat");
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 2) {
            showHelp(ctx);
            return;
        }

        String action = ctx.getArgs()[1].toLowerCase();

        switch (action) {
            case "create" -> handleCreate(ctx);
            case "edit" -> handleEdit(ctx);
            case "delete" -> handleDelete(ctx);
            case "list" -> handleList(ctx);
            default -> showHelp(ctx);
        }
    }

    private void handleCreate(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 3) {
            throw new CommandFailedException("Uso: /kit category create <id> [nome] [prioridade]");
        }

        String categoryId = ctx.getArgs()[2].toLowerCase();

        if (kitService.getCategory(categoryId) != null) {
            throw new CommandFailedException("Categoria já existe: " + categoryId);
        }

        String displayName = ctx.getArgs().length > 3 ? ctx.getArgs()[3] : categoryId;
        int priority = 0;
        if (ctx.getArgs().length > 4) {
            try {
                priority = Integer.parseInt(ctx.getArgs()[4]);
            } catch (NumberFormatException e) {
                throw new CommandFailedException("Prioridade deve ser um número!");
            }
        }

        KitCategory category = new KitCategory();
        category.setId(categoryId);
        category.setDisplayName(displayName);
        category.setPriority(priority);
        category.setIcon(Material.CHEST);

        kitService.saveCategory(category).thenAccept(saved -> ctx.getSender().sendMessage(StringUtils.text(
                "<green>Categoria <white>" + saved.getDisplayName() + " <green>criada com sucesso!")));
    }

    private void handleEdit(CommandContext ctx) throws CommandFailedException {
        if (!(ctx.getSender() instanceof Player player)) {
            throw new CommandFailedException("Este comando só pode ser executado por jogadores!");
        }

        if (ctx.getArgs().length < 3) {
            throw new CommandFailedException("Uso: /kit category edit <id>");
        }

        String categoryId = ctx.getArgs()[2].toLowerCase();
        KitCategory category = kitService.getCategory(categoryId);

        if (category == null) {
            throw new CommandFailedException("Categoria não encontrada: " + categoryId);
        }

        platform.getMenuManager().openToPlayer(player, KitCategoryEditorMenu.class,
                Map.of("category", (Object) category));
    }

    private void handleDelete(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 3) {
            throw new CommandFailedException("Uso: /kit category delete <id>");
        }

        String categoryId = ctx.getArgs()[2].toLowerCase();
        KitCategory category = kitService.getCategory(categoryId);

        if (category == null) {
            throw new CommandFailedException("Categoria não encontrada: " + categoryId);
        }

        int kitCount = kitService.getKitsByCategory(categoryId).size();
        if (kitCount > 0) {
            throw new CommandFailedException(
                    "Existem " + kitCount + " kits nesta categoria! Mova ou delete-os primeiro.");
        }

        kitService.deleteCategory(categoryId).thenAccept(v -> ctx.getSender().sendMessage(StringUtils.text(
                "<green>Categoria <white>" + category.getDisplayName() + " <green>deletada!")));
    }

    private void handleList(CommandContext ctx) {
        ctx.sendMessage(StringUtils.text("<gradient:#E0AAFF:#9D4EDD><bold>Categorias"));

        var categories = kitService.getAllCategories();
        if (categories.isEmpty()) {
            ctx.sendMessage(StringUtils.text("<gray>Nenhuma categoria cadastrada."));
            return;
        }

        for (KitCategory cat : categories) {
            int kitCount = kitService.getKitsByCategory(cat.getId()).size();
            ctx.sendMessage(StringUtils.text(
                    "<white>" + cat.getId() + " <gray>- <white>" + cat.getDisplayName() +
                            " <gray>(Kits: " + kitCount + ", Prioridade: " + cat.getPriority() + ")"));
        }
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(StringUtils.text("<gradient:#E0AAFF:#9D4EDD><bold>Category Commands"));
        ctx.sendMessage(
                StringUtils.text("<white>/kit category create <id> [nome] [prioridade] <gray>- Cria categoria"));
        ctx.sendMessage(StringUtils.text("<white>/kit category edit <id> <gray>- Edita categoria (icon, nome, etc)"));
        ctx.sendMessage(StringUtils.text("<white>/kit category delete <id> <gray>- Deleta categoria"));
        ctx.sendMessage(StringUtils.text("<white>/kit category list <gray>- Lista categorias"));
    }

    @Override
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 2) {
            return filterStartingWith(List.of("create", "edit", "delete", "list"), ctx.getArgs()[1]);
        }
        if (ctx.getArgs().length == 3) {
            String action = ctx.getArgs()[1].toLowerCase();
            if (action.equals("delete") || action.equals("edit")) {
                return filterStartingWith(
                        kitService.getAllCategories().stream().map(KitCategory::getId).toList(),
                        ctx.getArgs()[2]);
            }
        }
        return List.of();
    }
}

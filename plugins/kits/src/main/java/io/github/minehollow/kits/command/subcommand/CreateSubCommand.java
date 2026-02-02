package io.github.minehollow.kits.command.subcommand;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.menu.KitEditorMenu;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.sdk.util.time.Time;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class CreateSubCommand extends SimpleSubCommand {
    private final KitService kitService;
    private final BukkitPlatform platform;

    public CreateSubCommand(KitService kitService, BukkitPlatform platform) {
        super("create");
        this.kitService = kitService;
        this.platform = platform;
        this.permission = "kit.admin";
        this.playersOnly = true;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length < 4) {
            throw new CommandFailedException("Uso: /kit create <id> <categoria> <cooldown>");
        }

        String kitId = ctx.getArgs()[1].toLowerCase();
        String categoryId = ctx.getArgs()[2].toLowerCase();
        String cooldownStr = ctx.getArgs()[3];

        Kit existing = kitService.findKitByName(kitId);
        if (existing != null) {
            throw new CommandFailedException("Já existe um kit com este ID: " + kitId);
        }

        long cooldownSeconds;
        try {
            Time time = Time.parseString(cooldownStr);
            cooldownSeconds = (long) time.toSeconds();
        } catch (Exception e) {
            throw new CommandFailedException("Formato de cooldown inválido! Use: 1d2h30m, 1h, 30s");
        }

        // Auto-create category if not exists
        KitCategory category = kitService.getCategory(categoryId);
        if (category == null) {
            category = new KitCategory();
            category.setId(categoryId);
            category.setDisplayName(categoryId);
            category.setPriority(0);
            category.setIcon(Material.CHEST);
            kitService.saveCategory(category).join();
            ctx.getSender().sendMessage(StringUtils.text(
                    "<gray>Categoria <white>" + categoryId + " <gray>criada automaticamente."));
        }

        Kit newKit = new Kit();
        newKit.setId(kitId);
        newKit.setDisplayName(kitId);
        newKit.setCategoryId(categoryId);
        newKit.setCooldown(cooldownSeconds);
        newKit.setItems(List.of());

        platform.getMenuManager().openToPlayer(ctx.getSenderAsPlayer(), KitEditorMenu.class,
                Map.of("kit", newKit, "kitId", kitId));
    }

    @Override
    public List<String> performSubCommandTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 3) {
            return filterStartingWith(
                    kitService.getAllCategories().stream().map(KitCategory::getId).toList(),
                    ctx.getArgs()[2]);
        }
        return List.of();
    }
}

package io.github.minehollow.bestiary.command;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.archetype.MobArchetype;
import io.github.minehollow.bestiary.menu.ArchetypeListMenu;
import io.github.minehollow.bestiary.spawner.CustomMobSpawner;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.nbt.NbtUtil;
import io.github.minehollow.minecraft.util.range.IntRange;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MobEditCommand extends SimpleCommand {

    private final BestiaryPlugin plugin;

    public MobEditCommand(@NotNull BestiaryPlugin plugin) {
        super("mobedit", "bestiary.mobedit");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) {
        if (!(ctx.getSender() instanceof Player player)) {
            ctx.getSender().sendMessage("§cApenas jogadores podem usar este comando.");
            return;
        }

        String sub = Optional.ofNullable(ctx.getRawArgOrNull(0)).orElse("").toLowerCase();

        switch (sub) {
            case "" -> MenuUtil.openMenu(player, ArchetypeListMenu.class);
            case "createspawner" -> handleCreate(player, ctx);
            case "setamount", "settime" -> handleEdit(player, ctx, sub);
            default -> player.sendMessage("§cSubcomando desconhecido.");
        }
    }

    private void handleCreate(Player player, CommandContext ctx) {
        String id = ctx.getRawArgOrThrow(1, "Especifique o ID do archetype.");
        var archetype = plugin.getMobArchetypeService().getById(id);

        if (archetype == null) {
            player.sendMessage("§cNenhum archetype encontrado: " + id);
            return;
        }

        player.getInventory().addItem(CustomMobSpawner.createSpawnerItem(archetype.id()));
        player.sendMessage(StringUtils.formatString("<green>Spawner de <yellow>" + archetype.displayName() + " <green>gerado!"));
    }

    private void handleEdit(Player player, CommandContext ctx, String sub) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!CustomMobSpawner.hasSpawnerProperties(item)) {
            player.sendMessage("§cSegure um spawner customizado!");
            return;
        }

        String val = ctx.getRawArgOrThrow(1, "Forneça o valor para " + sub);

        try {
            NbtUtil.useItemPersistentData(item, container -> {
                if (sub.equals("setamount")) {
                    NbtUtil.setString(container, "bestiary", "spawn_amount", IntRange.parseString(val).toString());
                } else {
                    NbtUtil.setInt(container, "bestiary", "spawn_time", Integer.parseInt(val));
                }
            });
            player.sendMessage("§aPropriedade " + sub + " atualizada para: " + val);
        } catch (Exception e) {
            player.sendMessage("§cErro: Valor inválido.");
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return switch (ctx.getArgs().length) {
            case 1 -> Stream.of("createspawner", "setamount", "settime")
                .filter(s -> s.startsWith(ctx.getRawArgOrNull(0).toLowerCase())).toList();
            case 2 -> ctx.getRawArgOrNull(0).equalsIgnoreCase("createspawner")
                ? plugin.getMobArchetypeService().getAllCached().stream().map(MobArchetype::id).toList()
                : List.of();
            default -> List.of();
        };
    }
}

package io.github.minehollow.mines.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.menu.MineMainMenu;
import io.github.minehollow.mines.mine.MineDefinition;
import io.github.minehollow.mines.mine.MineSpawnPoint;
import io.github.minehollow.mines.pallet.LeveledBlockPalette;
import io.github.minehollow.mines.util.CachedBlockData;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MineCommand extends SimpleCommand {

    private static final List<String> ROOT_SUB_COMMANDS = List.of(
        "create",
        "delete",
        "list",
        "reset",
        "setglobal",
        "setmining",
        "setdisplay",
        "sethead",
        "setpalette",
        "setcurrencygain"
    );

    private final MinesPlugin plugin;

    public MineCommand(@NotNull MinesPlugin plugin) {
        super("mine", "mines.admin");
        this.playersOnly = true;
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            BukkitPlatform.getInstance().getMenuManager().openToPlayer(ctx.getSenderAsPlayer(), MineMainMenu.class);
            return;
        }

        switch (subCommand.toLowerCase(Locale.ROOT)) {
            case "create", "new" -> handleCreate(ctx);
            case "delete", "remove" -> handleDelete(ctx);
            case "list" -> handleList(ctx);
            case "reset" -> handleReset(ctx);
            case "setglobal", "setglobalarea" -> handleSetGlobalArea(ctx);
            case "setmining", "setmineablearea", "setmineable" -> handleSetMineableArea(ctx);
            case "setdisplay", "display" -> handleSetDisplayName(ctx);
            case "sethead", "head" -> handleSetHeadUrl(ctx);
            case "setpalette", "palette" -> handleSetPalette(ctx);
            case "setcurrencygain", "setcurrency", "currencygain", "currency" -> handleSetCurrencyGain(ctx);
            default -> sendUsage(ctx);
        }
    }

    private void handleCreate(@NotNull CommandContext ctx) throws CommandFailedException {
        final Player player = ctx.getSenderAsPlayer();
        final String rawId = ctx.getRawArgOrThrow(1, "§cUso: /mine create <mineId>");
        final String mineId = rawId.toLowerCase(Locale.ROOT);

        if (this.plugin.getDefinitionRegistry().findById(mineId) != null) {
            throw new CommandFailedException("§cJa existe uma mina com o id '" + mineId + "'.");
        }

        final SimpleCuboidArea selectedArea = this.readWorldEditSelection(player);
        final LeveledBlockPalette palette = this.defaultPalette();

        final MineDefinition definition = new MineDefinition(
            mineId,
            mineId,
            null,
            selectedArea,
            null,
            MineSpawnPoint.fromLocation(player.getLocation()),
            palette,
            new Object2LongArrayMap<>()
        );

        this.plugin.getDefinitionRegistry().upsert(definition);
        if (this.plugin.getDefinitionRegistry().getAll().size() == 1) {
            this.plugin.getDefinitionRegistry().setDefaultMineId(mineId);
            this.plugin.getConfig().set("default-mine-id", mineId);
        }

        this.saveDefinitions();
        ctx.sendMessage("§aMina '" + mineId + "' criada com sucesso.");
        ctx.sendMessage("§7Area global definida pela selecao do WorldEdit.");
        ctx.sendMessage("§eDefina a area de mineracao com: /mine setmining " + mineId);
    }

    private void handleDelete(@NotNull CommandContext ctx) throws CommandFailedException {
        final String mineId = ctx.getRawArgOrThrow(1, "§cUso: /mine delete <mineId>");
        final MineDefinition removed = this.plugin.getDefinitionRegistry().remove(mineId);
        if (removed == null) {
            throw new CommandFailedException("§cMina nao encontrada: '" + mineId + "'.");
        }

        final String currentDefault = this.plugin.getDefinitionRegistry().getDefaultMineId();
        if (currentDefault != null && currentDefault.equalsIgnoreCase(removed.getId())) {
            final String newDefault = this.plugin.getDefinitionRegistry().getAll().stream()
                .map(MineDefinition::getId)
                .findFirst()
                .orElse("default");

            this.plugin.getDefinitionRegistry().setDefaultMineId(newDefault);
            this.plugin.getConfig().set("default-mine-id", newDefault);
        }

        this.saveDefinitions();
        ctx.sendMessage("§aMina '" + removed.getId() + "' removida com sucesso.");
    }

    private void handleList(@NotNull CommandContext ctx) {
        final Collection<MineDefinition> all = this.plugin.getDefinitionRegistry().getAll();
        if (all.isEmpty()) {
            ctx.sendMessage("§7Nenhuma mina definida.");
            return;
        }

        ctx.sendMessage("§6Minas cadastradas (" + all.size() + "):");
        for (MineDefinition definition : all) {
            ctx.sendMessage("§7- §f" + definition.getId() + " §8(§7" + definition.getDisplayName() + "§8)");
        }
    }

    private void handleReset(@NotNull CommandContext ctx) throws CommandFailedException {
        final Player player = ctx.getSenderAsPlayer();
        final var activeInstance = this.plugin.getVirtualMineService().findByPlayer(player.getUniqueId());
        if (activeInstance == null) {
            throw new CommandFailedException("§cVoce nao possui uma mina ativa.");
        }

        final boolean reset = this.plugin.getVirtualMineService().reset(activeInstance.getOwnerId());
        if (!reset) {
            throw new CommandFailedException("§cNao foi possivel resetar a mina ativa.");
        }

        player.sendMessage(MinesPlugin.messages().mm("mine.reset"));
    }

    private void handleSetGlobalArea(@NotNull CommandContext ctx) throws CommandFailedException {
        final Player player = ctx.getSenderAsPlayer();
        final MineDefinition definition = this.requireDefinition(ctx, 1, "§cUso: /mine setglobal <mineId>");
        final SimpleCuboidArea selectedArea = this.readWorldEditSelection(player);

        final SimpleCuboidArea miningArea = definition.getMiningArea();
        if (miningArea != null && !containsArea(selectedArea, miningArea)) {
            throw new CommandFailedException("§cA nova area global precisa conter a area de mineracao atual.");
        }

        definition.setGlobalArea(selectedArea);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage("§aArea global da mina '" + definition.getId() + "' atualizada.");
    }

    private void handleSetMineableArea(@NotNull CommandContext ctx) throws CommandFailedException {
        final Player player = ctx.getSenderAsPlayer();
        final MineDefinition definition = this.requireDefinition(ctx, 1, "§cUso: /mine setmining <mineId>");
        final SimpleCuboidArea selectedArea = this.readWorldEditSelection(player);

        if (!containsArea(definition.getGlobalArea(), selectedArea)) {
            throw new CommandFailedException("§cA area de mineracao precisa estar dentro da area global.");
        }

        definition.setMiningArea(selectedArea);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage("§aArea de mineracao da mina '" + definition.getId() + "' atualizada.");
    }

    private void handleSetDisplayName(@NotNull CommandContext ctx) throws CommandFailedException {
        final MineDefinition definition = this.requireDefinition(ctx, 1, "§cUso: /mine setdisplay <mineId> <displayName>");
        final String displayName = ctx.joinArgs(2).trim();

        if (displayName.isEmpty()) {
            throw new CommandFailedException("§cO display name nao pode ser vazio.");
        }

        definition.setDisplayName(displayName);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage("§aDisplay name da mina '" + definition.getId() + "' atualizado.");
    }

    private void handleSetHeadUrl(@NotNull CommandContext ctx) throws CommandFailedException {
        final MineDefinition definition = this.requireDefinition(ctx, 1, "§cUso: /mine sethead <mineId> <url|none>");
        final String headInput = ctx.getRawArgOrThrow(2, "§cUso: /mine sethead <mineId> <url|none>").trim();

        if (headInput.equalsIgnoreCase("none") || headInput.equalsIgnoreCase("null") || headInput.equals("-")) {
            definition.setHeadUrl(null);
            this.plugin.getDefinitionRegistry().upsert(definition);
            this.saveDefinitions();
            ctx.sendMessage("§aHead URL removida da mina '" + definition.getId() + "'.");
            return;
        }

        definition.setHeadUrl(headInput);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage("§aHead URL da mina '" + definition.getId() + "' atualizada.");
    }

    private void handleSetPalette(@NotNull CommandContext ctx) throws CommandFailedException {
        final MineDefinition definition = this.requireDefinition(ctx, 1, "§cUso: /mine setpalette <mineId> <MATERIAL:chance,...>");
        final String rawPalette = ctx.joinArgs(2);

        final LeveledBlockPalette palette = this.parsePalette(rawPalette);
        definition.setBlockPalette(palette);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage("§aPaleta da mina '" + definition.getId() + "' atualizada.");
    }

    private void handleSetCurrencyGain(@NotNull CommandContext ctx) throws CommandFailedException {
        final MineDefinition definition = this.requireDefinition(
            ctx,
            1,
            "§cUso: /mine setcurrencygain <mineId> <currencyId> <valor|remove>"
        );

        final String currencyId = ctx.getRawArgOrThrow(
                2,
                "§cUso: /mine setcurrencygain <mineId> <currencyId> <valor|remove>"
            )
            .toLowerCase(Locale.ROOT);

        if (BukkitPlatform.getInstance().getCurrencyManager().getCurrency(currencyId) == null) {
            throw new CommandFailedException("§cCurrency invalida: '" + currencyId + "'.");
        }

        final String valueInput = ctx.getRawArgOrThrow(
            3,
            "§cUso: /mine setcurrencygain <mineId> <currencyId> <valor|remove>"
        ).trim();

        if (definition.getCurrencyGainValues() == null) {
            definition.setCurrencyGainValues(new Object2LongArrayMap<>());
        }

        final var gainValues = definition.getCurrencyGainValues();
        if (valueInput.equalsIgnoreCase("remove")
            || valueInput.equalsIgnoreCase("none")
            || valueInput.equalsIgnoreCase("null")
            || valueInput.equals("-")) {
            final boolean removed = gainValues.containsKey(currencyId);
            gainValues.removeLong(currencyId);

            this.plugin.getDefinitionRegistry().upsert(definition);
            this.saveDefinitions();

            if (removed) {
                ctx.sendMessage("§aGanho da currency '" + currencyId + "' removido da mina '" + definition.getId() + "'.");
            } else {
                ctx.sendMessage("§eNao havia ganho configurado para a currency '" + currencyId + "' nessa mina.");
            }
            return;
        }

        final long amount;
        try {
            amount = Long.parseLong(valueInput);
        } catch (NumberFormatException exception) {
            throw new CommandFailedException("§cValor invalido: '" + valueInput + "'. Use numero inteiro positivo ou 'remove'.");
        }

        if (amount <= 0L) {
            throw new CommandFailedException("§cO valor precisa ser maior que 0.");
        }

        gainValues.put(currencyId, amount);
        this.plugin.getDefinitionRegistry().upsert(definition);
        this.saveDefinitions();
        ctx.sendMessage(
            "§aGanho da currency '" + currencyId + "' na mina '" + definition.getId() + "' definido para §f" + amount + "§a por bloco."
        );
    }

    private @NotNull MineDefinition requireDefinition(
        @NotNull CommandContext ctx,
        int argIndex,
        @NotNull String usageMessage
    ) throws CommandFailedException {
        final String mineId = ctx.getRawArgOrThrow(argIndex, usageMessage);
        final MineDefinition definition = this.plugin.getDefinitionRegistry().findById(mineId);
        if (definition == null) {
            throw new CommandFailedException("§cMina nao encontrada: '" + mineId + "'.");
        }

        return definition;
    }

    private @NotNull SimpleCuboidArea readWorldEditSelection(@NotNull Player player) throws CommandFailedException {
        final String mineWorld = this.plugin.getRenderer().getWorldName();
        if (!player.getWorld().getName().equalsIgnoreCase(mineWorld)) {
            throw new CommandFailedException("§cVoce deve estar no mundo '" + mineWorld + "' para editar areas de mina.");
        }

        final Region region;
        try {
            final var actor = BukkitAdapter.adapt(player);
            final var session = WorldEdit.getInstance().getSessionManager().get(actor);
            region = session.getSelection(actor.getWorld());
        } catch (IncompleteRegionException exception) {
            throw new CommandFailedException("§cFaca uma selecao com o WorldEdit antes (//pos1 e //pos2).");
        }

        if (region == null) {
            throw new CommandFailedException("§cFaca uma selecao com o WorldEdit antes (//pos1 e //pos2).");
        }

        final var minimum = BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint());
        final var maximum = BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint());

        return new SimpleCuboidArea(
            minimum.getBlockX(), minimum.getBlockY(), minimum.getBlockZ(),
            maximum.getBlockX(), maximum.getBlockY(), maximum.getBlockZ()
        );
    }

    private @NotNull LeveledBlockPalette defaultPalette() {
        final MineDefinition anyDefinition = this.plugin.getDefinitionRegistry().getAll().stream().findFirst().orElse(null);
        if (anyDefinition != null && anyDefinition.getBlockPalette() != null) {
            return new LeveledBlockPalette(new LinkedHashMap<>(anyDefinition.getBlockPalette().blockChances()));
        }

        final Map<org.bukkit.block.data.BlockData, Double> blockChances = new LinkedHashMap<>();
        blockChances.put(CachedBlockData.get(Material.STONE), 100.0D);
        return new LeveledBlockPalette(blockChances);
    }

    private @NotNull LeveledBlockPalette parsePalette(@NotNull String rawPalette) throws CommandFailedException {
        final String trimmed = rawPalette.trim();
        if (trimmed.isEmpty()) {
            throw new CommandFailedException("§cPaleta vazia. Exemplo: STONE:70,COAL_ORE:30");
        }

        final Map<org.bukkit.block.data.BlockData, Double> blockChances = new LinkedHashMap<>();
        final String[] entries = trimmed.split(",");

        for (String entry : entries) {
            final String token = entry.trim();
            if (token.isEmpty()) {
                continue;
            }

            final int separatorIndex = token.indexOf(':');
            if (separatorIndex <= 0 || separatorIndex == token.length() - 1) {
                throw new CommandFailedException("§cFormato invalido em '" + token + "'. Use MATERIAL:chance");
            }

            final String materialName = token.substring(0, separatorIndex).trim().toUpperCase(Locale.ROOT);
            final String chanceText = token.substring(separatorIndex + 1).trim();

            final Material material = Material.matchMaterial(materialName);
            if (material == null || !material.isBlock()) {
                throw new CommandFailedException("§cMaterial invalido: " + materialName);
            }

            final double chance;
            try {
                chance = Double.parseDouble(chanceText);
            } catch (NumberFormatException exception) {
                throw new CommandFailedException("§cChance invalida para '" + materialName + "': " + chanceText);
            }

            if (chance <= 0.0D) {
                throw new CommandFailedException("§cA chance de '" + materialName + "' precisa ser maior que 0.");
            }

            blockChances.put(CachedBlockData.get(material), chance);
        }

        if (blockChances.isEmpty()) {
            throw new CommandFailedException("§cPaleta vazia. Exemplo: STONE:70,COAL_ORE:30");
        }

        return new LeveledBlockPalette(blockChances);
    }

    private void saveDefinitions() {
        this.plugin.getConfig().set("mines", null);
        final ConfigurationSection minesSection = this.plugin.getConfig().createSection("mines");

        final List<MineDefinition> definitions = new ArrayList<>(this.plugin.getDefinitionRegistry().getAll());
        definitions.sort((first, second) -> first.getId().compareToIgnoreCase(second.getId()));
        for (MineDefinition definition : definitions) {
            definition.writeToSection(minesSection, definition.getId());
        }

        this.plugin.saveConfig();
    }

    private void sendUsage(@NotNull CommandContext ctx) {
        ctx.sendMessage("§6Comandos de mina:");
        ctx.sendMessage("§e/mine §7- Abre o menu principal de minas");
        ctx.sendMessage("§e/mine list");
        ctx.sendMessage("§e/mine reset");
        ctx.sendMessage("§e/mine create <mineId> §7(requer selecao WorldEdit)");
        ctx.sendMessage("§e/mine delete <mineId>");
        ctx.sendMessage("§e/mine setglobal <mineId> §7(requer selecao WorldEdit)");
        ctx.sendMessage("§e/mine setmining <mineId> §7(requer selecao WorldEdit)");
        ctx.sendMessage("§e/mine setdisplay <mineId> <displayName>");
        ctx.sendMessage("§e/mine sethead <mineId> <url|none>");
        ctx.sendMessage("§e/mine setpalette <mineId> <MATERIAL:chance,...>");
        ctx.sendMessage("§e/mine setcurrencygain <mineId> <currencyId> <valor|remove>");
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) {
            final String prefix = ctx.getRawArgOrNull(0);
            return this.filterStartingWith(ROOT_SUB_COMMANDS, prefix);
        }

        final String subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            return Collections.emptyList();
        }

        final String normalized = subCommand.toLowerCase(Locale.ROOT);
        if (ctx.isArgsLength(2) && (
            normalized.equals("delete") ||
                normalized.equals("remove") ||
                normalized.equals("setglobal") ||
                normalized.equals("setglobalarea") ||
                normalized.equals("setmining") ||
                normalized.equals("setmineable") ||
                normalized.equals("setmineablearea") ||
                normalized.equals("setdisplay") ||
                normalized.equals("display") ||
                normalized.equals("sethead") ||
                normalized.equals("head") ||
                normalized.equals("setpalette") ||
                normalized.equals("palette") ||
                normalized.equals("setcurrencygain") ||
                normalized.equals("setcurrency") ||
                normalized.equals("currencygain") ||
                normalized.equals("currency")
        )) {
            final String prefix = ctx.getRawArgOrNull(1);
            final List<String> ids = this.plugin.getDefinitionRegistry().getAll().stream()
                .map(MineDefinition::getId)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
            return this.filterStartingWith(ids, prefix);
        }

        if (ctx.isArgsLength(3) && (
            normalized.equals("setcurrencygain") ||
                normalized.equals("setcurrency") ||
                normalized.equals("currencygain") ||
                normalized.equals("currency")
        )) {
            return this.filterStartingWith(
                BukkitPlatform.getInstance().getCurrencyManager().getAllCurrencyIds(),
                ctx.getRawArgOrNull(2)
            );
        }

        if (ctx.isArgsLength(3) && (normalized.equals("sethead") || normalized.equals("head"))) {
            return this.filterStartingWith(List.of("none"), ctx.getRawArgOrNull(2));
        }

        if (ctx.isArgsLength(3) && (normalized.equals("setpalette") || normalized.equals("palette"))) {
            return this.filterStartingWith(List.of("STONE:70,COAL_ORE:20,IRON_ORE:10"), ctx.getRawArgOrNull(2));
        }

        if (ctx.isArgsLength(4) && (
            normalized.equals("setcurrencygain") ||
                normalized.equals("setcurrency") ||
                normalized.equals("currencygain") ||
                normalized.equals("currency")
        )) {
            return this.filterStartingWith(List.of("1", "10", "100", "remove"), ctx.getRawArgOrNull(3));
        }

        return Collections.emptyList();
    }

    private static boolean containsArea(@NotNull SimpleCuboidArea outer, @NotNull SimpleCuboidArea inner) {
        return inner.getMinX() >= outer.getMinX() && inner.getMaxX() <= outer.getMaxX()
            && inner.getMinY() >= outer.getMinY() && inner.getMaxY() <= outer.getMaxY()
            && inner.getMinZ() >= outer.getMinZ() && inner.getMaxZ() <= outer.getMaxZ();
    }
}

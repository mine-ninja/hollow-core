package io.github.minehollow.bestiary.command;

import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import io.github.minehollow.bestiary.monster.MonsterManager;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.range.IntRange;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * /monster <subcommand> [args]
 * <p>
 * Subcomandos: create  <id> <entityType> <levelRange> <hpPerLevel> <dmgPerLevel> <defPerLevel> <displayName...> delete  <id> edit    <id> <field> <value>
 * spawn
 * <id> list info    <id>
 */
public class MonsterCommand extends SimpleCommand {

    private static final String PERM = "bestiary.admin";

    public MonsterCommand(CustomMonsterModelManager modelManager, MonsterManager monsterManager) {
        super("monster", PERM);
        this.playersOnly = true;
        this.subCommands = List.of(
            new CreateSub(modelManager),
            new DeleteSub(modelManager),
            new EditSub(modelManager),
            new SpawnSub(modelManager, monsterManager),
            new ListSub(modelManager),
            new InfoSub(modelManager)
        );
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) {
        ctx.sendMessage(
            "§6/monster create §e<id> <entityType> <levelRange> <hpPerLevel> <dmgPerLevel> <defPerLevel> <displayName...>",
            "§6/monster delete §e<id>",
            "§6/monster edit §e<id> <field> <value>",
            "§6/monster spawn §e<id>",
            "§6/monster list",
            "§6/monster info §e<id>"
        );
    }

    // -------------------------------------------------------------------------
    // create <id> <entityType> <levelRange> <hpPerLevel> <dmgPerLevel> <defPerLevel> <displayName...>
    // -------------------------------------------------------------------------

    private static class CreateSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        CreateSub(CustomMonsterModelManager modelManager) {
            super("create");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String id = ctx.getRawArgOrThrow(
                0, "§cUso: /monster create <id> <entityType> <levelRange> <hpPerLevel> <dmgPerLevel> <defPerLevel> <displayName...>");
            EntityType type = ctx.getEnumOrThrow(1, EntityType.class, "§cEntityType inválido. Ex: ZOMBIE, SKELETON");
            IntRange level = ctx.getArgOrThrow(2, IntRange::parseString, "§cLevelRange inválido. Ex: 1-10");
            DoubleRange hp = ctx.getArgOrThrow(3, DoubleRange::parseString, "§cHP por nível inválido. Ex: 10-20");
            DoubleRange dmg = ctx.getArgOrThrow(4, DoubleRange::parseString, "§cDano por nível inválido. Ex: 2-5");
            DoubleRange def = ctx.getArgOrThrow(5, DoubleRange::parseString, "§cDefesa por nível inválida. Ex: 0-3");
            String displayName = ctx.joinArgs(6);

            if (modelManager.getModelIfPresent(id) != null) {
                throw new CommandFailedException("§cJá existe um monstro com o id §e" + id + "§c.");
            }

            CustomMonsterModel model = new CustomMonsterModel(
                id, displayName, type, level, hp, dmg, def,
                new HashMap<>(), new HashMap<>(), 1.0, false
            );
            modelManager.registerModel(model);
            modelManager.saveModel(model);
            ctx.sendMessage("§aMonstro §e" + id + " §acriado com sucesso.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            if (ctx.getArgs().length == 2) {
                return filterStartingWith(
                    Arrays.stream(EntityType.values()).map(Enum::name).toList(),
                    ctx.getArgs()[1]
                );
            }
            return NONE_ARGS;
        }
    }

    // -------------------------------------------------------------------------
    // delete <id>
    // -------------------------------------------------------------------------

    private static class DeleteSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        DeleteSub(CustomMonsterModelManager modelManager) {
            super("delete");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String id = ctx.getRawArgOrThrow(0, "§cUso: /monster delete <id>");
            ctx.throwIfNull(modelManager.getModelIfPresent(id), "§cMonstro §e" + id + " §cnão encontrado.");
            modelManager.deleteModel(id);
            ctx.sendMessage("§aMonstro §e" + id + " §adeletado.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return filterStartingWith(modelIds(modelManager), ctx.getArgs().length > 0 ? ctx.getArgs()[0] : "");
        }

    }

    // -------------------------------------------------------------------------
    // edit <id> <field> <value>
    //   fields: displayname, entitytype, levelrange, hpperlevel, dmgperlevel, defperlevel
    // -------------------------------------------------------------------------

    private static class EditSub extends SimpleSubCommand {
        private static final List<String> FIELDS = List.of(
            "displayname", "entitytype", "levelrange", "hpperlevel", "dmgperlevel", "defperlevel", "scale"
        );
        private final CustomMonsterModelManager modelManager;

        EditSub(CustomMonsterModelManager modelManager) {
            super("edit");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String id = ctx.getRawArgOrThrow(0, "§cUso: /monster edit <id> <field> <value>");
            String field = ctx.getRawArgOrThrow(1, "§cEspecifique o campo: " + String.join(", ", FIELDS)).toLowerCase();
            String value = ctx.joinArgs(2);

            CustomMonsterModel model = ctx.throwIfNull(
                modelManager.getModelIfPresent(id), "§cMonstro §e" + id + " §cnão encontrado."
            );

            switch (field) {
                case "displayname" -> model.setDisplayName(value);
                case "entitytype" -> model.setEntityType(parseEnum(EntityType.class, value, "EntityType"));
                case "levelrange" -> model.setLevelRange(IntRange.parseString(value));
                case "hpperlevel" -> model.setHealthPerLevelRange(DoubleRange.parseString(value));
                case "dmgperlevel" -> model.setDamagePerLevelRange(DoubleRange.parseString(value));
                case "defperlevel" -> model.setDefensePerLevelRange(DoubleRange.parseString(value));
                case "scale" -> model.setScale(Double.parseDouble(value));
                default -> throw new CommandFailedException("§cCampo desconhecido: §e" + field);
            }

            modelManager.saveModel(model);
            ctx.sendMessage("§aCampo §e" + field + " §ado monstro §e" + id + " §aatualizado.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            String[] args = ctx.getArgs();
            return switch (args.length) {
                case 1 -> filterStartingWith(modelIds(modelManager), args[0]);
                case 2 -> filterStartingWith(FIELDS, args[1]);
                case 3 -> args[1].equalsIgnoreCase("entitytype")
                          ? filterStartingWith(Arrays.stream(EntityType.values()).map(Enum::name).toList(), args[2])
                          : NONE_ARGS;
                default -> NONE_ARGS;
            };
        }

        private <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String label) {
            try {
                return Enum.valueOf(cls, value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CommandFailedException("§c" + label + " inválido: §e" + value);
            }
        }
    }

    // -------------------------------------------------------------------------
    // spawn <id>  — spawna na posição do jogador
    // -------------------------------------------------------------------------

    private static class SpawnSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;
        private final MonsterManager monsterManager;

        SpawnSub(CustomMonsterModelManager modelManager, MonsterManager monsterManager) {
            super("spawn");
            this.permission = PERM;
            this.playersOnly = true;
            this.modelManager = modelManager;
            this.monsterManager = monsterManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String id = ctx.getRawArgOrThrow(0, "§cUso: /monster spawn <id>");
            ctx.throwIfNull(modelManager.getModelIfPresent(id), "§cMonstro §e" + id + " §cnão encontrado.");
            monsterManager.spawn(id, ctx.getSenderAsPlayer().getLocation());
            ctx.sendMessage("§aMonstro §e" + id + " §aspawnado.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return filterStartingWith(modelIds(modelManager), ctx.getArgs().length > 0 ? ctx.getArgs()[0] : "");
        }
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    private static class ListSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        ListSub(CustomMonsterModelManager modelManager) {
            super("list");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            var models = modelManager.getAllModels();
            if (models.isEmpty()) {
                ctx.sendMessage("§cNenhum monstro registrado.");
                return;
            }
            ctx.sendMessage("§6Monstros registrados §7(" + models.size() + ")§6:");
            for (CustomMonsterModel m : models) {
                ctx.sendMessage("§e" + m.getId() + " §7- §f" + m.getDisplayName()
                                + " §7(" + m.getEntityType().name() + ") Lv." + m.getLevelRange());
            }
        }
    }

    // -------------------------------------------------------------------------
    // info <id>
    // -------------------------------------------------------------------------

    private static class InfoSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        InfoSub(CustomMonsterModelManager modelManager) {
            super("info");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String id = ctx.getRawArgOrThrow(0, "§cUso: /monster info <id>");
            CustomMonsterModel m = ctx.throwIfNull(
                modelManager.getModelIfPresent(id), "§cMonstro §e" + id + " §cnão encontrado."
            );
            ctx.sendMessage(
                "§6Monstro: §e" + m.getId(),
                "§7Nome: §f" + m.getDisplayName(),
                "§7Tipo: §f" + m.getEntityType().name(),
                "§7Nível: §f" + m.getLevelRange(),
                "§7HP/nível: §f" + m.getHealthPerLevelRange(),
                "§7Dano/nível: §f" + m.getDamagePerLevelRange(),
                "§7Defesa/nível: §f" + m.getDefensePerLevelRange(),
                "§7Drops: §f" + m.getPossibleDrops().size()
            );
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return filterStartingWith(modelIds(modelManager), ctx.getArgs().length > 0 ? ctx.getArgs()[0] : "");
        }
    }

    // -------------------------------------------------------------------------
    // Utilitário compartilhado
    // -------------------------------------------------------------------------

    private static List<String> modelIds(CustomMonsterModelManager mgr) {
        return mgr.getAllModels()
            .stream().map(CustomMonsterModel::getId).toList();
    }
}
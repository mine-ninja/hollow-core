package io.github.minehollow.bestiary.command;

import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import io.github.minehollow.bestiary.monster.MonsterManager;
import io.github.minehollow.bestiary.monster.ability.AbilityDefinition;
import io.github.minehollow.bestiary.monster.ability.AbilityType;
import io.github.minehollow.bestiary.monster.ability.DamageRange;
import io.github.minehollow.bestiary.monster.goal.MobBehavior;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.range.IntRange;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
            new InfoSub(modelManager),
            new AbilityCreateSub(modelManager),
            new AbilityDeleteSub(modelManager),
            new AbilityEditSub(modelManager),
            new AbilityListSub(modelManager)
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
            "§6/monster info §e<id>",
            "§6/monster abilitycreate §e<monsterId> <abilityId> <type> <damage> <cooldown> <range> [displayName...]",
            "§6/monster abilitydelete §e<monsterId> <abilityId>",
            "§6/monster abilityedit §e<monsterId> <abilityId> <field> <value>",
            "§6/monster abilitylist §e<monsterId>"
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
                new HashMap<>(), new HashMap<>(), 1.0, false,
                MobBehavior.DEFAULT_AGGRESSIVE, new java.util.ArrayList<>()
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
    // abilitycreate <monsterId> <abilityId> <type> <damage> <cooldown> <range> [displayName...]
    // -------------------------------------------------------------------------

    private static class AbilityCreateSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        AbilityCreateSub(CustomMonsterModelManager modelManager) {
            super("abilitycreate");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String monsterId = ctx.getRawArgOrThrow(0,
                "§cUso: /monster abilitycreate <monsterId> <abilityId> <type> <damage> <cooldown> <range> [displayName...]");
            String abilityId = ctx.getRawArgOrThrow(1,
                "§cEspecifique o ID da ability.");
            AbilityType type = ctx.getEnumOrThrow(2, AbilityType.class,
                "§cTipo inválido. Ex: PROJECTILE, AOE, TARGETED");
            DamageRange damage = ctx.getArgOrThrow(3, DamageRange::parse,
                "§cDano inválido. Ex: 5.0-12.0");
            long cooldown = ctx.getArgOrThrow(4, Long::parseLong,
                "§cCooldown inválido (ms). Ex: 5000");
            double range = ctx.getArgOrThrow(5, Double::parseDouble,
                "§cRange inválido. Ex: 16.0");
            String displayName = ctx.getArgs().length > 6 ? ctx.joinArgs(6) : abilityId;

            CustomMonsterModel model = ctx.throwIfNull(
                modelManager.getModelIfPresent(monsterId),
                "§cMonstro §e" + monsterId + " §cnão encontrado."
            );

            // Check for duplicate ability id
            for (AbilityDefinition existing : model.getAbilities()) {
                if (existing.getId().equalsIgnoreCase(abilityId)) {
                    throw new CommandFailedException("§cJá existe uma ability com o id §e" + abilityId + " §cneste monstro.");
                }
            }

            AbilityDefinition ability = new AbilityDefinition(
                abilityId, displayName, type, damage, cooldown, range,
                3.0, 0.8, null, null
            );

            // Ensure the list is mutable
            java.util.ArrayList<AbilityDefinition> abilities = new java.util.ArrayList<>(model.getAbilities());
            abilities.add(ability);
            model.setAbilities(abilities);
            modelManager.saveModel(model);
            ctx.sendMessage("§aAbility §e" + abilityId + " §aadicionada ao monstro §e" + monsterId + "§a.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            String[] args = ctx.getArgs();
            return switch (args.length) {
                case 1 -> filterStartingWith(modelIds(modelManager), args[0]);
                case 3 -> filterStartingWith(
                    Arrays.stream(AbilityType.values()).map(Enum::name).toList(), args[2]);
                default -> NONE_ARGS;
            };
        }
    }

    // -------------------------------------------------------------------------
    // abilitydelete <monsterId> <abilityId>
    // -------------------------------------------------------------------------

    private static class AbilityDeleteSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        AbilityDeleteSub(CustomMonsterModelManager modelManager) {
            super("abilitydelete");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String monsterId = ctx.getRawArgOrThrow(0,
                "§cUso: /monster abilitydelete <monsterId> <abilityId>");
            String abilityId = ctx.getRawArgOrThrow(1,
                "§cEspecifique o ID da ability.");

            CustomMonsterModel model = ctx.throwIfNull(
                modelManager.getModelIfPresent(monsterId),
                "§cMonstro §e" + monsterId + " §cnão encontrado."
            );

            java.util.ArrayList<AbilityDefinition> abilities = new java.util.ArrayList<>(model.getAbilities());
            boolean removed = abilities.removeIf(a -> a.getId().equalsIgnoreCase(abilityId));
            if (!removed) {
                throw new CommandFailedException("§cAbility §e" + abilityId + " §cnão encontrada no monstro §e" + monsterId + "§c.");
            }

            model.setAbilities(abilities);
            modelManager.saveModel(model);
            ctx.sendMessage("§aAbility §e" + abilityId + " §aremovida do monstro §e" + monsterId + "§a.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            String[] args = ctx.getArgs();
            return switch (args.length) {
                case 1 -> filterStartingWith(modelIds(modelManager), args[0]);
                case 2 -> {
                    CustomMonsterModel model = modelManager.getModelIfPresent(args[0]);
                    if (model == null) yield NONE_ARGS;
                    yield filterStartingWith(
                        model.getAbilities().stream().map(AbilityDefinition::getId).toList(),
                        args[1]
                    );
                }
                default -> NONE_ARGS;
            };
        }
    }

    // -------------------------------------------------------------------------
    // abilityedit <monsterId> <abilityId> <field> <value>
    //   fields: displayname, type, damage, cooldown, range, radius, speed, particle, sound
    // -------------------------------------------------------------------------

    private static class AbilityEditSub extends SimpleSubCommand {
        private static final List<String> FIELDS = List.of(
            "displayname", "type", "damage", "cooldown", "range",
            "radius", "speed", "particle", "sound"
        );
        private final CustomMonsterModelManager modelManager;

        AbilityEditSub(CustomMonsterModelManager modelManager) {
            super("abilityedit");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String monsterId = ctx.getRawArgOrThrow(0,
                "§cUso: /monster abilityedit <monsterId> <abilityId> <field> <value>");
            String abilityId = ctx.getRawArgOrThrow(1,
                "§cEspecifique o ID da ability.");
            String field = ctx.getRawArgOrThrow(2,
                "§cEspecifique o campo: " + String.join(", ", FIELDS)).toLowerCase();
            String value = ctx.joinArgs(3);

            CustomMonsterModel model = ctx.throwIfNull(
                modelManager.getModelIfPresent(monsterId),
                "§cMonstro §e" + monsterId + " §cnão encontrado."
            );

            // Find the ability and its index
            java.util.ArrayList<AbilityDefinition> abilities = new java.util.ArrayList<>(model.getAbilities());
            int index = -1;
            AbilityDefinition old = null;
            for (int i = 0; i < abilities.size(); i++) {
                if (abilities.get(i).getId().equalsIgnoreCase(abilityId)) {
                    index = i;
                    old = abilities.get(i);
                    break;
                }
            }
            if (old == null) {
                throw new CommandFailedException("§cAbility §e" + abilityId + " §cnão encontrada no monstro §e" + monsterId + "§c.");
            }

            // Rebuild with the edited field
            String displayName = old.getDisplayName();
            AbilityType type = old.getType();
            DamageRange damage = old.getDamageRange();
            long cooldown = old.getCooldown();
            double range = old.getRange();
            double radius = old.getRadius();
            double speed = old.getSpeed();
            Particle particle = old.getParticle();
            Sound sound = old.getSound();

            switch (field) {
                case "displayname" -> displayName = value;
                case "type" -> type = parseEnum(AbilityType.class, value, "AbilityType");
                case "damage" -> damage = DamageRange.parse(value);
                case "cooldown" -> cooldown = Long.parseLong(value);
                case "range" -> range = Double.parseDouble(value);
                case "radius" -> radius = Double.parseDouble(value);
                case "speed" -> speed = Double.parseDouble(value);
                case "particle" -> particle = value.equalsIgnoreCase("none") ? null
                    : parseEnum(Particle.class, value, "Particle");
                case "sound" -> {
                    if (value.equalsIgnoreCase("none")) {
                        sound = null;
                    } else {
                        sound = AbilityDefinition.safeSound(value);
                        if (sound == null)
                            throw new CommandFailedException("§cSound inválido: §e" + value);
                    }
                }
                default -> throw new CommandFailedException("§cCampo desconhecido: §e" + field);
            }

            AbilityDefinition updated = new AbilityDefinition(
                old.getId(), displayName, type, damage, cooldown,
                range, radius, speed, particle, sound
            );
            abilities.set(index, updated);
            model.setAbilities(abilities);
            modelManager.saveModel(model);
            ctx.sendMessage("§aCampo §e" + field + " §ada ability §e" + abilityId
                + " §ado monstro §e" + monsterId + " §aatualizado.");
        }

        @Override
        @SuppressWarnings("removal")
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            String[] args = ctx.getArgs();
            return switch (args.length) {
                case 1 -> filterStartingWith(modelIds(modelManager), args[0]);
                case 2 -> {
                    CustomMonsterModel model = modelManager.getModelIfPresent(args[0]);
                    if (model == null) yield NONE_ARGS;
                    yield filterStartingWith(
                        model.getAbilities().stream().map(AbilityDefinition::getId).toList(),
                        args[1]
                    );
                }
                case 3 -> filterStartingWith(FIELDS, args[2]);
                case 4 -> {
                    String f = args[2].toLowerCase();
                    if (f.equals("type")) {
                        yield filterStartingWith(
                            Arrays.stream(AbilityType.values()).map(Enum::name).toList(), args[3]);
                    }
                    if (f.equals("particle")) {
                        yield filterStartingWith(
                            Arrays.stream(Particle.values()).map(Enum::name).toList(), args[3]);
                    }
                    if (f.equals("sound")) {
                        yield filterStartingWith(
                            org.bukkit.Registry.SOUNDS.stream()
                                .map(s -> s.key().value())
                                .toList(), args[3]);
                    }
                    yield NONE_ARGS;
                }
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
    // abilitylist <monsterId>
    // -------------------------------------------------------------------------

    private static class AbilityListSub extends SimpleSubCommand {
        private final CustomMonsterModelManager modelManager;

        AbilityListSub(CustomMonsterModelManager modelManager) {
            super("abilitylist");
            this.permission = PERM;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            String monsterId = ctx.getRawArgOrThrow(0,
                "§cUso: /monster abilitylist <monsterId>");

            CustomMonsterModel model = ctx.throwIfNull(
                modelManager.getModelIfPresent(monsterId),
                "§cMonstro §e" + monsterId + " §cnão encontrado."
            );

            List<AbilityDefinition> abilities = model.getAbilities();
            if (abilities.isEmpty()) {
                ctx.sendMessage("§cNenhuma ability registrada para o monstro §e" + monsterId + "§c.");
                return;
            }
            ctx.sendMessage("§6Abilities do monstro §e" + monsterId + " §7(" + abilities.size() + ")§6:");
            for (AbilityDefinition a : abilities) {
                ctx.sendMessage("§e" + a.getId() + " §7- §f" + a.getDisplayName()
                    + " §7[" + a.getType().name() + "] §7Dmg:" + a.getDamageRange()
                    + " §7CD:" + a.getCooldown() + "ms §7Range:" + a.getRange());
            }
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
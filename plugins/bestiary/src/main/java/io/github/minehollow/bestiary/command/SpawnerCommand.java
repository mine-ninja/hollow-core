package io.github.minehollow.bestiary.command;

import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import io.github.minehollow.bestiary.spawner.MonsterSpawner;
import io.github.minehollow.bestiary.spawner.SpawnerManager;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.command.subcommand.SimpleSubCommand;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /spawner <subcommand> [args]
 * <p>
 * Subcomandos: create  <id> <modelId> <intervalSec> <radius> <maxAlive> <spawnCount> delete  <id> edit    <id> <field> <value> tp      <id> list info    <id>
 */
public class SpawnerCommand extends SimpleCommand {

    private static final String PERM = "bestiary.admin";

    public SpawnerCommand(SpawnerManager spawnerManager, CustomMonsterModelManager modelManager) {
        super("monsterspawner", PERM);
        this.playersOnly = true;
        this.subCommands = List.of(
            new CreateSub(spawnerManager, modelManager),
            new DeleteSub(spawnerManager),
            new EditSub(spawnerManager, modelManager),
            new TpSub(spawnerManager),
            new ListSub(spawnerManager),
            new InfoSub(spawnerManager)
        );
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) {
        ctx.sendMessage(
            "§6/monsterspawner create §e<id> <modelId> <intervalSec> <radius> <maxAlive> <spawnCount>",
            "§6/monsterspawner delete §e<id>",
            "§6/monsterspawner edit §e<id> <field> <value>",
            "§6/monsterspawner tp §e<id>",
            "§6/monsterspawner list",
            "§6/monsterspawner info §e<id>"
        );
    }

    // -------------------------------------------------------------------------
    // create <id> <modelId> <intervalSec> <radius> <maxAlive> <spawnCount>
    // -------------------------------------------------------------------------

    private static class CreateSub extends SimpleSubCommand {
        private final SpawnerManager spawnerManager;
        private final CustomMonsterModelManager modelManager;

        CreateSub(SpawnerManager spawnerManager, CustomMonsterModelManager modelManager) {
            super("create");
            this.permission = PERM;
            this.playersOnly = true;
            this.spawnerManager = spawnerManager;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            final String usage = "§cUso: /spawner create <id> <modelId> <intervalSec> <radius> <maxAlive> <spawnCount>";
            String id = ctx.getRawArgOrThrow(0, usage);
            String modelId = ctx.getRawArgOrThrow(1, usage);
            double interval = ctx.getDoubleOrThrow(2, "§cintervaloSec deve ser um número. Ex: 30.0");
            double radius = ctx.getDoubleOrThrow(3, "§cRadius deve ser um número. Ex: 20.0");
            int maxAlive = ctx.getIntOrThrow(4, "§cmaxAlive deve ser inteiro. Ex: 5");
            int spawnCount = ctx.getIntOrThrow(5, "§cspawnCount deve ser inteiro. Ex: 2");

            ctx.throwIfNull(
                modelManager.getModelIfPresent(modelId),
                "§cMonstro §e" + modelId + " §cnão encontrado."
            );

            // Verifica id duplicado
            boolean exists = spawnerManager.getAll()
                .stream()
                .anyMatch(s -> s.getUniqueId().toString().equals(id) || s.getUniqueId().toString().startsWith(id));
            // id é UUID gerado — usamos o argumento como nome/prefixo para lookup futuro; aqui geramos UUID novo
            Player player = ctx.getSenderAsPlayer();

            MonsterSpawner spawner = new MonsterSpawner(
                UUID.randomUUID(),
                player.getLocation(),
                modelId,
                interval,
                radius,
                maxAlive,
                spawnCount
            );

            spawnerManager.addSpawner(spawner);
            ctx.sendMessage("§aSpawner criado em §e" + formatLoc(player) + " §acon UUID §e" + spawner.getUniqueId() + "§a.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            if (ctx.getArgs().length == 2) {
                return filterStartingWith(modelIds(modelManager), ctx.getArgs()[1]);
            }
            return NONE_ARGS;
        }
    }

    // -------------------------------------------------------------------------
    // delete <uuid>
    // -------------------------------------------------------------------------

    private static class DeleteSub extends SimpleSubCommand {
        private final SpawnerManager spawnerManager;

        DeleteSub(SpawnerManager spawnerManager) {
            super("delete");
            this.permission = PERM;
            this.spawnerManager = spawnerManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            UUID uuid = parseUUID(ctx.getRawArgOrThrow(0, "§cUso: /spawner delete <uuid>"));
            spawnerManager.removeSpawner(uuid);
            ctx.sendMessage("§aSpawner §e" + uuid + " §adeletado.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return ctx.getArgs().length == 1
                   ? filterStartingWith(spawnerUUIDs(spawnerManager), ctx.getArgs()[0])
                   : NONE_ARGS;
        }
    }

    // -------------------------------------------------------------------------
    // edit <uuid> <field> <value>
    //   fields: modelid, interval, radius, maxalive, spawncount
    // -------------------------------------------------------------------------

    private static class EditSub extends SimpleSubCommand {
        private static final List<String> FIELDS = List.of(
            "modelid", "interval", "radius", "maxalive", "spawncount"
        );
        private final SpawnerManager spawnerManager;
        private final CustomMonsterModelManager modelManager;

        EditSub(SpawnerManager spawnerManager, CustomMonsterModelManager modelManager) {
            super("edit");
            this.permission = PERM;
            this.spawnerManager = spawnerManager;
            this.modelManager = modelManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            UUID uuid = parseUUID(ctx.getRawArgOrThrow(0, "§cUso: /spawner edit <uuid> <field> <value>"));
            String field = ctx.getRawArgOrThrow(1, "§cCampos: " + String.join(", ", FIELDS)).toLowerCase();
            String value = ctx.getRawArgOrThrow(2, "§cForneça um valor.");

            MonsterSpawner spawner = ctx.throwIfNull(
                findSpawner(spawnerManager, uuid), "§cSpawner §e" + uuid + " §cnão encontrado."
            );

            switch (field) {
                case "modelid" -> {
                    ctx.throwIfNull(modelManager.getModelIfPresent(value), "§cModelo §e" + value + " §cnão encontrado.");
                    spawner.setMonsterModelId(value);
                }
                case "interval" -> spawner.setSpawnIntervalSeconds(parseDouble(value, "interval"));
                case "radius" -> spawner.setActivationRadius(parseDouble(value, "radius"));
                case "maxalive" -> spawner.setMaxAlive(parseInt(value, "maxalive"));
                case "spawncount" -> spawner.setSpawnCount(parseInt(value, "spawncount"));
                default -> throw new CommandFailedException("§cCampo desconhecido: §e" + field);
            }

            ctx.sendMessage("§aCampo §e" + field + " §aatualizado.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            String[] args = ctx.getArgs();
            return switch (args.length) {
                case 1 -> filterStartingWith(spawnerUUIDs(spawnerManager), args[0]);
                case 2 -> filterStartingWith(FIELDS, args[1]);
                case 3 -> "modelid".equalsIgnoreCase(args[1])
                          ? filterStartingWith(modelIds(modelManager), args[2])
                          : NONE_ARGS;
                default -> NONE_ARGS;
            };
        }

        private double parseDouble(String v, String field) {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
                throw new CommandFailedException("§c" + field + " deve ser numérico.");
            }
        }

        private int parseInt(String v, String field) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new CommandFailedException("§c" + field + " deve ser inteiro.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // tp <uuid> — teleporta o jogador até o spawner
    // -------------------------------------------------------------------------

    private static class TpSub extends SimpleSubCommand {
        private final SpawnerManager spawnerManager;

        TpSub(SpawnerManager spawnerManager) {
            super("tp");
            this.permission = PERM;
            this.playersOnly = true;
            this.spawnerManager = spawnerManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            UUID uuid = parseUUID(ctx.getRawArgOrThrow(0, "§cUso: /spawner tp <uuid>"));
            MonsterSpawner spawner = ctx.throwIfNull(
                findSpawner(spawnerManager, uuid), "§cSpawner §e" + uuid + " §cnão encontrado."
            );
            ctx.getSenderAsPlayer().teleport(spawner.getLocation());
            ctx.sendMessage("§aTeleportado para o spawner §e" + uuid + "§a.");
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return ctx.getArgs().length == 1
                   ? filterStartingWith(spawnerUUIDs(spawnerManager), ctx.getArgs()[0])
                   : NONE_ARGS;
        }
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    private static class ListSub extends SimpleSubCommand {
        private final SpawnerManager spawnerManager;

        ListSub(SpawnerManager spawnerManager) {
            super("list");
            this.permission = PERM;
            this.spawnerManager = spawnerManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            var all = spawnerManager.getAll();
            if (all.isEmpty()) {
                ctx.sendMessage("§cNenhum spawner registrado.");
                return;
            }
            ctx.sendMessage("§6Spawners §7(" + all.size() + ")§6:");
            for (MonsterSpawner s : all) {
                ctx.sendMessage("§e" + s.getUniqueId()
                                + " §7| §f" + s.getMonsterModelId()
                                + " §7| " + formatLoc(s));
            }
        }
    }

    // -------------------------------------------------------------------------
    // info <uuid>
    // -------------------------------------------------------------------------

    private static class InfoSub extends SimpleSubCommand {
        private final SpawnerManager spawnerManager;

        InfoSub(SpawnerManager spawnerManager) {
            super("info");
            this.permission = PERM;
            this.spawnerManager = spawnerManager;
        }

        @Override
        protected void performSubCommand(CommandContext ctx) {
            UUID uuid = parseUUID(ctx.getRawArgOrThrow(0, "§cUso: /spawner info <uuid>"));
            MonsterSpawner s = ctx.throwIfNull(
                findSpawner(spawnerManager, uuid), "§cSpawner §e" + uuid + " §cnão encontrado."
            );
            ctx.sendMessage(
                "§6Spawner: §e" + s.getUniqueId(),
                "§7Modelo: §f" + s.getMonsterModelId(),
                "§7Local: §f" + formatLoc(s),
                "§7Intervalo: §f" + s.getSpawnIntervalSeconds() + "s",
                "§7Raio: §f" + s.getActivationRadius(),
                "§7Max vivos: §f" + s.getMaxAlive(),
                "§7Por ciclo: §f" + s.getSpawnCount()
            );
        }

        @Override
        public List<String> performSubCommandTabComplete(CommandContext ctx) {
            return ctx.getArgs().length == 1
                   ? filterStartingWith(spawnerUUIDs(spawnerManager), ctx.getArgs()[0])
                   : NONE_ARGS;
        }
    }


    private static UUID parseUUID(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException("§cUUID inválido: §e" + raw);
        }
    }

    private static MonsterSpawner findSpawner(SpawnerManager mgr, UUID uuid) {
        return mgr.getAll()
            .stream().filter(s -> s.getUniqueId().equals(uuid)).findFirst().orElse(null);
    }

    private static List<String> spawnerUUIDs(SpawnerManager mgr) {
        return mgr.getAll()
            .stream().map(s -> s.getUniqueId().toString()).toList();
    }

    private static List<String> modelIds(CustomMonsterModelManager mgr) {
        return mgr.getAllModels()
            .stream().map(CustomMonsterModel::getId).toList();
    }

    private static String formatLoc(Player p) {
        final var loc = p.getLocation();

        return p.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    private static String formatLoc(MonsterSpawner s) {
        var l = s.getLocation();
        return l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
    }
}
package net.warcane.lugin.core.minecraft.punish.api;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.audience.Audience;
import net.warcane.lugin.core.database.MongoCounterService;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.punish.api.message.PunishMessagePubSub;
import net.warcane.lugin.core.minecraft.punish.core.PunishLogger;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.MessageManager;
import net.warcane.lugin.core.minecraft.punish.core.database.redis.RedisDatabase;
import net.warcane.lugin.core.minecraft.punish.events.PlayerPunishEvents;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.data.*;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import net.warcane.lugin.core.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
@Log4j2
public class PunishManager {

    private static HashMap<UUID, PunishedDTO> cache = new HashMap<>();

    private static PunishManager instance;
    private final MongoCollection<PunishedDTO> collection;
    private final ExecutorService executorService;

    @Getter
    private PunishLogger punishLogger;

    private static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool();


    public PunishManager(Plugin plugin, ExecutorService executorService) {
        this.punishLogger = new PunishLogger(plugin.getDataFolder());
        this.collection = MongoDbConnector.getInstance().getCollection("punished_players", PunishedDTO.class);
        this.executorService = executorService;
        Bukkit.getPluginManager().registerEvents(new PlayerPunishEvents(), plugin);
    }

    public void loadPlayer(Player player) {
        getPunishedPlayer(player.getUniqueId()).whenComplete((dto, throwable) -> {
            if (throwable != null) {
                Bukkit.getLogger().severe("Erro ao buscar jogador punido: " + throwable.getMessage());
                return;
            }

            if (dto == null)
                dto = new PunishedDTO(player.getName(), player.getUniqueId(), new ArrayList<>());

            cache.put(player.getUniqueId(), dto);
        });
    }

    public void unloadPlayer(Player player) {
        cache.remove(player.getUniqueId());
    }

    public void punishPlayer(PlayerAccount target, Player punisher, PunishmentInfo punishmentInfo, String message) {
        Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(punisher);
        int nextId = MongoCounterService.get().getNextIdFor("punishment_info_id");

        StringUtils.send(audience, "<l-confirm>Punição aplicada ao jogador <l-yellow>" + target.playerName() + " <l-green> com sucesso! ID: " + nextId);

        if (message == null) {
            message = "Não anexada.";
        }

        String finalMessage = message;
        createOrAddNewPunish(target, punishmentInfo, message, punisher, nextId).whenComplete((punishment, throwable) -> {
            if (throwable != null) {
                log.error(throwable);
                return;
            }

            Player player = Bukkit.getPlayer(target.uniqueId());

            if (player != null) {
                String time = MessageUtils.formatMilliseconds(punishment.getExpiresAt() - System.currentTimeMillis());
                player.sendMessage(" ");
                player.sendMessage("§cVocê foi silenciado. Sua punição irá expirar em " + time);
                player.sendMessage(" ");
                player.sendMessage("§c * Motivo: " + punishmentInfo.title());
                player.sendMessage("§c * Prova: " + finalMessage);
                player.sendMessage("§c * ID: #" + nextId);
                player.sendMessage(" ");
                player.sendMessage("§cCaso ache que a sua punição tenha sido aplicada de maneira incorreta acesse §ediscord.gg/lugin§c para criar uma revisão.");
                player.sendMessage(" ");
            }
        });
    }

    private CompletableFuture<PunishedDTO.Punishment> createOrAddNewPunish(PlayerAccount target, PunishmentInfo punishInfo, String proofLink, Player punisher, int id) {
        UUID uuid = target.uniqueId();
        CompletableFuture<PunishedDTO.Punishment> completableFuture = new CompletableFuture<>();

        getPunishedPlayer(uuid).whenComplete((punished, e) -> {
            if (e != null) {
                Bukkit.getLogger().severe("Erro ao buscar jogador punido: " + e.getMessage());
                return;
            }

            int repeatCount = 0;
            if (punished == null) {
                punished = new PunishedDTO(target.playerName(), uuid, new ArrayList<>());
            } else {
                for (PunishedDTO.Punishment p : punished.getPunishments()) {
                    if (p.getPunishmentInfoId() != punishInfo.id()) continue;
                    if (p.getStatus() == PunishmentStatus.REVOKED) continue;
                    repeatCount++;
                }
            }

            Tuple<PunishTime, PunishmentType> punishmentTimeAndType = punishInfo.getPunishment(repeatCount);
            long timePunished = punishmentTimeAndType.a().getTimeInMilliseconds();
            if (timePunished > 0) {
                timePunished = System.currentTimeMillis() + timePunished;
            }

            Player player = Bukkit.getPlayer(uuid); // Todo: Ask Matheus to save ip address in PlayerAccount
            String ipAddress = player != null ? player.getAddress().getHostString() : "unknown";
            PunishedDTO.Punishment punishment = new PunishedDTO.Punishment(
                id,
                ipAddress,
                repeatCount,
                punishInfo.id(),
                proofLink,
                System.currentTimeMillis(),
                timePunished,
                punisher.getUniqueId()
            );
            punishment.setStatus(PunishmentStatus.ACTIVE);

            punished.getPunishments().add(punishment);

            collection.replaceOne(Filters.eq("uuid", uuid), punished, new ReplaceOptions().upsert(true));
            MessageManager.get().sendMessage(new PunishMessagePubSub(target.uniqueId(), target.playerName(), punishment));

            punishLogger.logPunish(punishment, punisher.getName(), punishInfo, target.playerName(), target.uniqueId());

            cache.put(uuid, punished);

            completableFuture.complete(punishment);
        });

        return completableFuture;
    }

    public PunishedDTO getCachedPlayer(UUID uuid) {
        return cache.get(uuid);
    }

    public CompletableFuture<PunishedDTO> getPunishedPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                collection.find(Filters.eq("uuid", uuid)).first()
            , SHARED_EXECUTOR);
    }

    public CompletableFuture<PunishedDTO> getPunishedPlayer(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return SHARED_EXECUTOR.submit(() -> collection.find(Filters.eq("name", name)).first()).get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Query timed out after 5 seconds", e);
            } catch (Exception e) {
                throw new RuntimeException("Query failed", e);
            }
        }, SHARED_EXECUTOR);
    }

    public CompletableFuture<PunishedDTO.Punishment> getPunishmentById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            PunishedDTO punished = collection.find(
                Filters.elemMatch("punishments", Filters.eq("_id", id))
            ).first();

            if (punished != null) {
                for (PunishedDTO.Punishment p : punished.getPunishments()) {
                    if (p.getId() == id) return p;
                }
            }

            throw new IllegalArgumentException("Punishment with ID " + id + " not found.");
        });
    }

    private CompletableFuture<PunishedDTO> getPunishedByPunishmentId(int id) {
        return CompletableFuture.supplyAsync(() ->
                collection.find(Filters.elemMatch("punishments", Filters.eq("_id", id))).first()
            , SHARED_EXECUTOR);
    }

    public void updatePunishmentStatus(int id, PunishedDTO.Punishment updatedPunishment) {
        getPunishedByPunishmentId(id).whenComplete((punished, e) -> {
            if (e != null) {
                Bukkit.getLogger().severe("Erro ao buscar jogador punido: " + e.getMessage());
                return;
            }
            if (punished == null) {
                Bukkit.getLogger().warning("Punição com ID " + id + " não encontrada.");
                return;
            }

            for (int i = 0; i < punished.getPunishments().size(); i++) {
                PunishedDTO.Punishment punishment = punished.getPunishments().get(i);
                if (punishment.getId() == id) {
                    punished.getPunishments().set(i, updatedPunishment);
                    break;
                }
            }

            collection.replaceOne(Filters.eq("uuid", punished.getUuid()), punished, new ReplaceOptions().upsert(true));
            MessageManager.get().sendMessage(new PunishMessagePubSub(punished.getUuid(), punished.getName(), updatedPunishment));

            if (cache.containsKey(punished.getUuid()))
                cache.put(punished.getUuid(), punished);
        });
    }

    // --- Utility Methods ---
    public static boolean checkLink(String message) {
        return message.startsWith("http://") || message.startsWith("https://");
    }

    public static void init(Plugin plugin, ExecutorService executorService) {
        if (instance != null) {
            throw new IllegalStateException("PunishManager is already initialized.");
        }

        RedisDatabase.init();
        MessageManager.init(plugin);
        instance = new PunishManager(plugin, executorService);
    }

    public static PunishManager get() {
        if (instance == null) throw new IllegalStateException("PunishManager not initialized!");
        return instance;
    }
}

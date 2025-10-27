package net.warcane.lugin.core.minigames.party;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.MinigamesPlatformPlugin;
import net.warcane.lugin.core.minigames.party.data.PartyData;
import net.warcane.lugin.core.minigames.party.data.PartyMember;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.party.PartyAcceptPacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyDenyPacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyExpiredInvitePacket;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.SetParams;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PartyService {

    public static final String REDIS_KEY = "party";
    private static final String LEADER_KEY = REDIS_KEY + ":leader";
    private static final String LEADER_INFO_KEY = REDIS_KEY + ":leader_info";
    private static final int LEADER_LEASE_SECONDS = 30;
    private static final int LEADER_CHECK_SECONDS = 10;
    private static final String KEY_EVENT_PATTERN = "__keyevent@*__:expired";
    private static final String EXPIRY_PREFIX = REDIS_KEY + ":exp:";
    private static final String MEMBER_KEY = REDIS_KEY + ":member:";

    private boolean isLeader = false;

    private final MinigamesPlatform platform;
    private final RedisConnector redisConnector;
    private final NetworkClient networkClient;
    private final BukkitAudiences audiences;

    public PartyService(MinigamesPlatform platform) {
        this.platform = platform;
        this.redisConnector = RedisConnector.getInstance();
        this.networkClient = platform.getNetworkClient();
        this.audiences = MinigamesPlatformPlugin.getInstance().adventure();
        this.startExpiryWorker();
    }

    private void startExpiryWorker() {
        this.startKeyspaceNotifications();
        Bukkit.getScheduler().runTaskTimer(platform.getPlugin(), this::tryBecomeLeader, 0L, LEADER_CHECK_SECONDS * 20L);
    }

    private void startKeyspaceNotifications() {
        redisConnector.useJedis(j -> j.configSet("notify-keyspace-events", "Ex"));

        var keyspaceThread = new Thread(() ->
            redisConnector.useJedis(jedis ->
                jedis.psubscribe(new JedisPubSub() {
                    @Override
                    public void onPMessage(String pattern, String channel, String message) {
                        if (message.startsWith(EXPIRY_PREFIX) && isLeader) {
                            //party:exp:invite:SrSheep_:bytcode
                            var idStr = message.split(":");
                            try {
                                if (idStr[2].equalsIgnoreCase("invite")) {
                                    var senderName = idStr[3];
                                    var receiverName = idStr[4];
                                    Tasks.runSync(() -> processExpiredInviteParty(senderName, receiverName));
                                }
                            } catch (Exception e) {
                                log.error("Error processing expired key: {}", e.getMessage());
                            }
                        }
                    }
                }, KEY_EVENT_PATTERN)));

        keyspaceThread.setDaemon(true);
        keyspaceThread.start();
    }

    private void tryBecomeLeader() {
        var serverId = platform.getId();
        var isMainServer = this.platform.isMainServer();

        var acquired = redisConnector.supplyFromJedis(j -> {
            var currentLeader = j.get(LEADER_KEY);
            var leaderInfo = j.get(LEADER_INFO_KEY);
            var currentLeaderIsMain = leaderInfo != null && leaderInfo.contains("main:true");

            if (isMainServer && currentLeader != null && !currentLeader.equals(serverId) && !currentLeaderIsMain) {
                j.set(LEADER_KEY, serverId, SetParams.setParams().ex(LEADER_LEASE_SECONDS));
                j.set(LEADER_INFO_KEY, "main:true", SetParams.setParams().ex(LEADER_LEASE_SECONDS));
                return true;
            }

            if (currentLeader != null && currentLeader.equals(serverId)) {
                j.expire(LEADER_KEY, LEADER_LEASE_SECONDS);
                j.expire(LEADER_INFO_KEY, LEADER_LEASE_SECONDS);
                return true;
            }

            var result = j.set(LEADER_KEY, serverId, SetParams.setParams().nx().ex(LEADER_LEASE_SECONDS));
            if ("OK".equals(result)) {
                j.set(LEADER_INFO_KEY, "main:" + isMainServer, SetParams.setParams().ex(LEADER_LEASE_SECONDS));
                return true;
            }

            return false;
        });

        if (acquired != this.isLeader) {
            this.isLeader = acquired;
            if (this.isLeader) {
                log.info("This server is now the party expiry leader{}", isMainServer ? " (main server)" : "");
            }
        }
    }

    public void createPartyInvite(String senderName, String receiverName) {
        redisConnector.useJedis(jedis -> {
            var inviteExpiryKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            jedis.setex(inviteExpiryKey, 60, "");
        });
    }

    public void removePartyInvite(String senderName, String receiverName) {
        redisConnector.useJedis(jedis -> {
            var inviteKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            jedis.del(inviteKey);
        });
    }

    public boolean partyInviteExists(String senderName, String receiverName) {
        return redisConnector.supplyFromJedis(jedis -> {
            var inviteKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            return jedis.exists(inviteKey);
        });
    }

    private void processExpiredInviteParty(String senderName, String receiverName) {

        var senderAccount = platform.getPlayerAccountService().getPlayerAccountByName(senderName);
        var receiverAccount = platform.getPlayerAccountService().getPlayerAccountByName(receiverName);

        CompletableFuture.allOf(senderAccount, receiverAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var receiverAcc = receiverAccount.join();

                if (senderAcc == null && receiverAcc == null) {
                    return;
                }

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyExpiredInvitePacket(
                    senderAcc.uniqueId(),
                    "§cO pedido de party enviado para " + receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §cexpirou."
                ));

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyExpiredInvitePacket(
                    receiverAcc.uniqueId(),
                    "§eO pedido de party de " + receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §eexpirou."
                ));
            });
    }

    public void acceptPartyInvite(String senderName, Player receiver) {
        //TODO: Tratar casos de erro de Party cheia.

        validatePartyInviteExists(senderName, receiver.getName());

        if (isPlayerInParty(receiver.getName())) {
            StringUtils.send(receiver, "<red>Você já está em uma party. Saia da sua party atual para aceitar um novo convite.");
            return;
        }

        var senderAccount = platform.getPlayerAccountService().getPlayerAccountByName(senderName);
        var receiverAccount = platform.getPlayerAccountService().getPlayerAccount(receiver.getUniqueId());

        CompletableFuture.allOf(senderAccount, receiverAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var receiverAcc = receiverAccount.join();

                if (senderAcc == null) {
                    StringUtils.send(receiver, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (receiverAcc == null) {
                    StringUtils.send(receiver, "<red>O jogador não está online.");
                    return;
                }

                var partyMember = new PartyMember(receiver.getName(), receiverAcc.uniqueId());
                var lender = new PartyMember(senderAcc.playerName(), senderAcc.uniqueId());
                this.processPartyInviteAccept(lender, partyMember);

                new ComponentBuilder()
                    .simple("§aVocê aceitou o pedido de party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§a.")
                    .send(audiences.player(receiver));

                var targetBukkitPlayer = Bukkit.getPlayer(senderAcc.uniqueId());

                if (targetBukkitPlayer != null) {
                    new ComponentBuilder()
                        .simple(receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §aaceitou seu pedido de party!")
                        .send(audiences.player(targetBukkitPlayer));
                } else {
                    networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyAcceptPacket(
                        senderAcc.uniqueId(),
                        receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §aaceitou seu pedido de party!"
                    ));
                }
            });
    }

    public void denyPartyInvite(String senderName, Player receiver) {
        validatePartyInviteExists(senderName, receiver.getName());

        var senderAccount = platform.getPlayerAccountService().getPlayerAccountByName(senderName);
        var receiverAccount = platform.getPlayerAccountService().getPlayerAccount(receiver.getUniqueId());

        CompletableFuture.allOf(senderAccount, receiverAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var receiverAcc = receiverAccount.join();

                if (senderAcc == null) {
                    StringUtils.send(receiver, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (receiverAcc == null) {
                    StringUtils.send(receiver, "<red>O jogador não está online.");
                    return;
                }

                new ComponentBuilder()
                    .simple("§cVocê recusou o pedido de party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§c.")
                    .send(audiences.player(receiver));

                var targetBukkitPlayer = Bukkit.getPlayer(senderAcc.uniqueId());

                if (targetBukkitPlayer != null) {
                    new ComponentBuilder()
                        .simple(receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §crecusou seu pedido de party!")
                        .send(audiences.player(targetBukkitPlayer));
                } else {
                    networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyDenyPacket(
                        senderAcc.uniqueId(),
                        receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §crecusou seu pedido de party!"
                    ));
                }

                this.removePartyInvite(senderName, receiver.getName());
            });
    }

    private void validatePartyInviteExists(String senderName, String receiverName) {
        if (!partyInviteExists(senderName, receiverName)) {
            throw new CommandFailedException("§cNão há um convite pendente de " + senderName + ".");
        }
    }

    private void processPartyInviteAccept(PartyMember leader, PartyMember receiver) {
        redisConnector.useJedis(jedis -> {
            var party = findOrCreateParty(leader);

            if (party.members().size() == 5) {
                throw new CommandFailedException("§cA party de " + leader.name() + " está cheia.");
            }

            party.members().add(receiver);

            var partyKey = REDIS_KEY + ":party:" + party.partyId();
            jedis.set(partyKey, party.toJson());
            jedis.set(MEMBER_KEY + receiver.name().toLowerCase(), party.partyId());
        });

        this.removePartyInvite(leader.name().toLowerCase(), receiver.name());
    }

    private PartyData findParty(String playerName) {
        return redisConnector.supplyFromJedis(jedis -> {
            var partyId = jedis.get(MEMBER_KEY + playerName.toLowerCase());
            var partyKey = REDIS_KEY + ":party:" + partyId;
            var partyData = jedis.get(partyKey);
            if (partyData != null) {
                return PartyData.fromJson(partyData);
            }
            return null;
        });
    }

    private PartyData findOrCreateParty(PartyMember leader) {
        var leaderName = leader.name();
        var party = findParty(leaderName);
        if (party != null) {
            return party;
        }

        var id = UUID.randomUUID().toString().substring(0, 6);
        log.info("Creating new party. {}", id);
        var newParty = new PartyData(id, leader, new HashSet<>(), false);
        redisConnector.useJedis(jedis -> {
            var partyKey = REDIS_KEY + ":party:" + newParty.partyId();
            jedis.set(partyKey, newParty.toJson());

            jedis.set(MEMBER_KEY + leaderName.toLowerCase(), newParty.partyId());
        });
        return newParty;
    }

    public void leaveParty(Player player) {
        var playerName = player.getName();
        var party = findParty(playerName);

        if (party == null) {
            throw new CommandFailedException("§cVocê não está em uma party.");
        }

        if (party.leader().name().equalsIgnoreCase(playerName)) {
            throw new CommandFailedException("§cO líder da party não pode sair da party. Transfira a liderança ou delete a party.");
        }

        redisConnector.useJedis(jedis -> {
            party.members().removeIf(member -> member.name().equalsIgnoreCase(playerName));

            var partyKey = REDIS_KEY + ":party:" + party.partyId();
            jedis.set(partyKey, party.toJson());
            jedis.del(MEMBER_KEY + playerName.toLowerCase());

            player.sendMessage("§cVocê saiu da party.");

            //TODO: Enviar notificação para os outros membros da party que o jogador saiu.
        });
    }

    public void deleteParty(Player player) {
        var leaderName = player.getName();
        var party = findParty(leaderName);
        if (party == null) {
            throw new CommandFailedException("§cVocê não está em uma party.");
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            throw new CommandFailedException("§cApenas o líder da party pode deletá-la.");
        }

        redisConnector.useJedis(jedis -> {
            for (var memberName : party.members()) {
                jedis.del(MEMBER_KEY + memberName.name().toLowerCase());
            }

            jedis.del(MEMBER_KEY + leaderName.toLowerCase());

            var partyKey = REDIS_KEY + ":party:" + party.partyId();
            jedis.del(partyKey);

            player.sendMessage("§cVocê deletou a party.");

            //TODO: Enviar notificação para os outros membros da party que a party foi deletada.
            //TODO: Acredito que essa ainda não é a melhor forma de fazer isso, mas por enquanto tá ok.
        });
    }

    public void sendPartyInfo(Player player) {
        var playerName = player.getName();
        var party = findParty(playerName);
        if (party == null) {
            throw new CommandFailedException("§cVocê não está em uma party.");
        }

        platform.getPlayerAccountService().fetchPlayerAccountList(party.members().stream().map(PartyMember::uniqueId).toList())
            .thenComposeAsync(accounts -> platform.getPlayerAccountService().getPlayerAccount(party.leader().uniqueId())
                .thenAcceptAsync(leaderAcc -> {
                    if (leaderAcc == null) {
                        StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                        return;
                    }

                    var memberNames = accounts.stream()
                        .map(acc -> acc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Nenhum membro");

                    new ComponentBuilder()
                        .newLine()
                        .simple("§eInformações da Party:")
                        .newLine()
                        .newLine()
                        .simple("§f▪ §eDono:")
                        .newLine()
                        .simple("§7- " + leaderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))
                        .newLine()
                        .simple("§f▪ §eID da Party:")
                        .newLine()
                        .simple("§7- §f" + party.partyId())
                        .newLine()
                        .simple("§f▪ §eMembros (" + party.members().size() + "):")
                        .newLine()
                        .simple("§7- ")
                        .simple(memberNames)
                        .newLine()
                        .simple("§f▪ §eVagas:")
                        .newLine()
                        .simple("§7- §f " + getMaxPartySizeForGroup(leaderAcc))
                        .newLine()
                        .simple("§f▪ §eTipo:")
                        .newLine()
                        .simple("§7- " + (party.isOpen() ? "§aPublica" : "§cPrivado"))
                        .send(audiences.player(player));
                }));
    }

    public boolean isPlayerInParty(String playerName) {
        return redisConnector.supplyFromJedis(jedis ->
            jedis.exists(MEMBER_KEY + playerName.toLowerCase())
        );
    }

    private int getMaxPartySizeForGroup(PlayerAccount account) {
        return switch (account.getHighestSubscription().group()) {
            case INFLUENCER, HELPER, MODERATOR, ADMIN, MANAGER, MASTER -> 60;
            case SUPREME -> 20;
            case LEGENDARY -> 15;
            case CHAMPION -> 10;
            default -> 5;
        };
    }
}

package net.warcane.lugin.core.minigames.party;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.MinigamesPlatformPlugin;
import net.warcane.lugin.core.minigames.party.data.PartyData;
import net.warcane.lugin.core.minigames.party.data.PartyMember;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.party.PartyDeletedPacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyExpiredInvitePacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyLeaderMessagePacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyMessagePacket;
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

                if (senderAcc == null || receiverAcc == null) {
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

    public void acceptPartyInvite(String senderName, Player player) {
        validatePartyInviteExists(senderName, player);

        if (isPlayerInParty(player.getName())) {
            StringUtils.send(player, "<red>Você já está em uma party. Saia da sua party atual para aceitar um novo convite.");
            return;
        }

        var senderAccount = platform.getPlayerAccountService().getPlayerAccountByName(senderName);
        var receiverAccount = platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId());

        CompletableFuture.allOf(senderAccount, receiverAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var receiverAcc = receiverAccount.join();

                if (senderAcc == null) {
                    StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (receiverAcc == null) {
                    StringUtils.send(player, "<red>O jogador não está online.");
                    return;
                }

                var partyMember = new PartyMember(player.getName(), receiverAcc.uniqueId());
                var lender = new PartyMember(senderAcc.playerName(), senderAcc.uniqueId());
                var party = this.processPartyInviteAccept(lender, partyMember);

                if (party.members().size() == getMaxPartySizeForGroup(senderAcc)) {
                    StringUtils.send(player, "§cA party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§c está cheia.");
                    return;
                }

                new ComponentBuilder()
                    .simple("§aVocê aceitou o pedido de party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§a.")
                    .send(audiences.player(player));

                var targetBukkitPlayer = Bukkit.getPlayer(senderAcc.uniqueId());
                var message = new ComponentBuilder()
                    .simple(receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §aaceitou seu pedido de party!");

                if (targetBukkitPlayer != null) {
                    message.send(audiences.player(targetBukkitPlayer));
                } else {
                    networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(), message.build()));
                }
            });
    }

    public void denyPartyInvite(String senderName, Player receiver) {
        validatePartyInviteExists(senderName, receiver);

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
                    networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyLeaderMessagePacket(
                        senderAcc.uniqueId(),
                        receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §crecusou seu pedido de party!"
                    ));
                }

                this.removePartyInvite(senderName, receiver.getName());
            });
    }

    public void transferPartyLeadership(String targetName, Player player) {
        var leaderName = player.getName();
        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            StringUtils.send(player, "§cApenas o líder da party pode transferir a liderança.");
            return;
        }

        var targetMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (targetMember == null) {
            StringUtils.send(player, "§cO jogador " + targetName + " §cnão está na sua party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(targetMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for transferring party leadership: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var updateParty = party.removeMember(targetName).addMember(party.leader()).setLeader(targetMember);

                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.set(partyKey, updateParty.toJson());

                player.sendMessage("§aVocê transferiu a liderança da party para " + account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§a.");

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    new ComponentBuilder()
                        .simple(account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §eé o novo líder da party.")
                        .build()
                ));
            });
        });
    }

    public void leaveParty(Player player) {
        var playerName = player.getName();
        var party = findPartyPlayer(playerName);

        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (party.leader().name().equalsIgnoreCase(playerName)) {
            StringUtils.send(player, "§cO líder da party não pode sair da party. Transfira a liderança ou delete a party.");
            return;
        }

        var partyMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(playerName)).findFirst().orElse(null);

        if (partyMember == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(partyMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for leaving party: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var updatedParty = party.removeMember(playerName);

                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.set(partyKey, updatedParty.toJson());
                jedis.del(MEMBER_KEY + playerName.toLowerCase());

                player.sendMessage("§cVocê saiu da party.");

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    new ComponentBuilder()
                        .simple(account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §csaiu da party.")
                        .build()
                ));
            });
        });
    }

    public void deleteParty(Player player) {
        var leaderName = player.getName();
        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            StringUtils.send(player, "§cApenas o líder da party pode deletá-la.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(party.leader().uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for deleting party: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.del(partyKey);
                party.members().forEach(member -> jedis.del(MEMBER_KEY + member.name().toLowerCase()));
                jedis.del(MEMBER_KEY + party.leader().name().toLowerCase());

                player.sendMessage("§cVocê deletou a party.");

                var message = account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §cdesfez a party.";
                var memberNames = party.members().stream().map(PartyMember::name).toList();
                var memberNamesNotOnlineThisServer = new HashSet<String>();

                memberNames.forEach(memberName -> {
                    var targetPlayer = Bukkit.getPlayer(memberName);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(message);
                    } else {
                        memberNamesNotOnlineThisServer.add(memberName);
                    }
                });

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyDeletedPacket(
                    memberNamesNotOnlineThisServer.stream().toList(),
                    message
                ));
            });
        });
    }

    public void kickParty(String targetName, Player player) {
        var leaderName = player.getName();
        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            StringUtils.send(player, "§cApenas o líder da party pode expulsar membros.");
            return;
        }

        var targetMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (targetMember == null) {
            StringUtils.send(player, "§cO jogador " + targetName + " §cnão está na sua party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(targetMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for kicking party member: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var updatedParty = party.removeMember(targetName);

                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.set(partyKey, updatedParty.toJson());
                jedis.del(MEMBER_KEY + targetName.toLowerCase());

                player.sendMessage("§eVocê expulsou " + account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §eda party.");

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    new ComponentBuilder()
                        .simple(account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §cfoi expulso da party.")
                        .build(),
                    true
                ));
            });
        });
    }

    public void sendPartyInfo(Player player) {
        var playerName = player.getName();
        var party = findPartyPlayer(playerName);
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
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

    public void sendPartyChatMessage(Player player, String mensagem) {
        var party = findPartyPlayer(player.getName());
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).thenAcceptAsync(senderAccount -> {
            if (senderAccount == null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                return;
            }

            var formattedMessage = StringUtils.formatString("§d[Party] " + senderAccount.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§f: " + mensagem);
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(), formattedMessage));
        }).exceptionally(ex -> {
            StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
            log.error("Error fetching player account for party chat message: {}", ex.getMessage());
            return null;
        });
    }

    public void sendPartyChatMessageToMember(String partyId, Component message, boolean excludeLeader) {
        var party = findPartyById(partyId);
        if (party == null) {
            return;
        }

        var partyMembers = party.members();

        if (!excludeLeader) {
            partyMembers.add(party.leader());
        }

        for (var member : partyMembers) {
            var targetPlayer = Bukkit.getPlayer(member.uniqueId());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                audiences.player(targetPlayer).sendMessage(message);
            }
        }
    }

    public void openParty(Player player) {
        var party = findPartyPlayer(player.getName());
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(player.getName())) {
            StringUtils.send(player, "§cApenas o líder da party pode alterar o status da party.");
            return;
        }

        if (party.isOpen()) {
            StringUtils.send(player, "§cA party já está aberta.");
            return;
        }

        var updatedParty = party.setOpen();

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for opening party: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.set(partyKey, updatedParty.toJson());

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    StringUtils.formatString(account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §etornou a party pública."),
                    true
                ));

                for (var onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (party.members().stream().noneMatch(m -> m.name().equalsIgnoreCase(onlinePlayer.getName()))) {
                        StringUtils.send(onlinePlayer, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §etornou a party pública. Utilize o comando /party entrar para entrar em sua party.");
                    }
                }
            });
        });
    }

    public void closeParty(Player player) {
        var party = findPartyPlayer(player.getName());
        if (party == null) {
            StringUtils.send(player, "§cVocê não está em uma party.");
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(player.getName())) {
            StringUtils.send(player, "§cApenas o líder da party pode alterar o status da party.");
            return;
        }

        if (!party.isOpen()) {
            StringUtils.send(player, "§cA party já está fechada.");
            return;
        }

        var updatedParty = party.setClose();

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                log.error("Error fetching player account for closing party: {}", ex.getMessage());
                return;
            }

            redisConnector.useJedis(jedis -> {
                var partyKey = REDIS_KEY + ":party:" + party.partyId();
                jedis.set(partyKey, updatedParty.toJson());

                StringUtils.send(player, "§aVocê fechou a party para novos membros.");

                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    StringUtils.formatString(account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §ctornou a party privada."),
                    true
                ));
            });
        });
    }

    public void joinInParty(Player player, String leaderName) {
        if (isPlayerInParty(player.getName())) {
            StringUtils.send(player, "§cVocê já está em uma party. Saia da sua party atual para entrar em uma nova.");
            return;
        }

        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, "§cO jogador " + leaderName + " §cnão está liderando nenhuma party.");
            return;
        }

        if (!party.isOpen()) {
            StringUtils.send(player, "§cA party de " + leaderName + " §cestá fechada para novos membros.");
            return;
        }

        var leaderAccount = platform.getPlayerAccountService().getPlayerAccountByName(leaderName);
        var playerAccount = platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId());

        CompletableFuture.allOf(leaderAccount, playerAccount)
            .thenRunAsync(() -> {
                var leaderAcc = leaderAccount.join();
                var playerAcc = playerAccount.join();

                if (leaderAcc == null || playerAcc == null) {
                    StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (party.members().size() >= getMaxPartySizeForGroup(leaderAcc)) {
                    StringUtils.send(player, "§cA party de " + leaderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§c está cheia.");
                    return;
                }

                var partyMember = new PartyMember(player.getName(), playerAcc.uniqueId());
                var updatedParty = party.addMember(partyMember);

                redisConnector.useJedis(jedis -> {
                    var partyKey = REDIS_KEY + ":party:" + party.partyId();
                    jedis.set(partyKey, updatedParty.toJson());
                    jedis.set(MEMBER_KEY + player.getName().toLowerCase(), party.partyId());

                    StringUtils.send(player, "§aVocê entrou na party de " + leaderName + "§a.");

                    networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                        StringUtils.formatString(playerAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §aentrou na party."))
                    );
                });
            });
    }

    private void validatePartyInviteExists(String senderName, Player player) {
        if (!partyInviteExists(senderName, player.getName())) {
            StringUtils.send(player, "§cNão há um convite pendente de " + senderName + ".");
        }
    }

    private PartyData processPartyInviteAccept(PartyMember leader, PartyMember receiver) {
        return redisConnector.supplyFromJedis(jedis -> {
            var party = findOrCreateParty(leader);

            var updateParty = party.addMember(receiver);

            var partyKey = REDIS_KEY + ":party:" + party.partyId();
            jedis.set(partyKey, updateParty.toJson());
            jedis.set(MEMBER_KEY + receiver.name().toLowerCase(), party.partyId());
            this.removePartyInvite(leader.name().toLowerCase(), receiver.name());

            return party;
        });
    }

    public PartyData findPartyPlayer(String playerName) {
        return redisConnector.supplyFromJedis(jedis -> {
            var partyId = jedis.get(MEMBER_KEY + playerName.toLowerCase());
            if (partyId == null) {
                return null;
            }
            return findPartyById(partyId);
        });
    }

    private PartyData findPartyById(String partyId) {
        return redisConnector.supplyFromJedis(jedis -> {
            var partyKey = REDIS_KEY + ":party:" + partyId;
            var partyData = jedis.get(partyKey);
            return partyData != null ? PartyData.fromJson(partyData) : null;
        });
    }

    private PartyData findOrCreateParty(PartyMember leader) {
        var leaderName = leader.name();
        var party = findPartyPlayer(leaderName);
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

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

    private boolean isLeader = false;

    private final MinigamesPlatform platform;
    private final RedisConnector redisConnector;
    private final NetworkClient networkClient;
    private final BukkitAudiences audiences;
    private final PartyRepository partyRepository;

    public PartyService(MinigamesPlatform platform) {
        this.platform = platform;
        this.redisConnector = RedisConnector.getInstance();
        this.networkClient = platform.getNetworkClient();
        this.audiences = MinigamesPlatformPlugin.getInstance().adventure();
        this.partyRepository = new PartyRepository(this.redisConnector);
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
        partyRepository.createPartyInvite(senderName, receiverName);
    }

    public void removePartyInvite(String senderName, String receiverName) {
        partyRepository.removePartyInvite(senderName, receiverName);
    }

    public boolean partyInviteExists(String senderName, String receiverName) {
        return partyRepository.partyInviteExists(senderName, receiverName);
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
            StringUtils.send(player, PartyMessages.ALREADY_IN_PARTY);
            return;
        }

        var senderAccount = platform.getPlayerAccountService().getPlayerAccountByName(senderName);
        var receiverAccount = platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId());

        CompletableFuture.allOf(senderAccount, receiverAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var receiverAcc = receiverAccount.join();

                if (senderAcc == null) {
                    StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                    return;
                }

                if (receiverAcc == null) {
                    StringUtils.send(player, PartyMessages.PLAYER_NOT_ONLINE);
                    return;
                }

                var partyMember = new PartyMember(player.getName(), receiverAcc.uniqueId());
                var lender = new PartyMember(senderAcc.playerName(), senderAcc.uniqueId());
                var party = this.processPartyInviteAccept(lender, partyMember);

                if (party.members().size() == getMaxPartySizeForGroup(senderAcc)) {
                    StringUtils.send(player, PartyMessages.PARTY_FULL);
                    return;
                }

                StringUtils.send(player, "§aVocê aceitou o pedido de party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§a.");

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
                    StringUtils.send(receiver, PartyMessages.PROCESS_ERROR);
                    return;
                }

                if (receiverAcc == null) {
                    StringUtils.send(receiver, PartyMessages.PLAYER_NOT_ONLINE);
                    return;
                }

                StringUtils.send(receiver, "§cVocê recusou o pedido de party de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§c.");

                var targetBukkitPlayer = Bukkit.getPlayer(senderAcc.uniqueId());

                if (targetBukkitPlayer != null) {
                    StringUtils.send(targetBukkitPlayer, receiverAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §crecusou seu pedido de party!");
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
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            StringUtils.send(player, PartyMessages.ONLY_LEADER);
            return;
        }

        var targetMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (targetMember == null) {
            StringUtils.send(player, "§cO jogador " + targetName + " §cnão está na sua party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(targetMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                log.error("Error fetching player account for transferring party leadership: {}", ex.getMessage());
                return;
            }

            var updatedParty = partyRepository.transferLeadership(party, targetMember);
            if (updatedParty == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            player.sendMessage("§aVocê transferiu a liderança da party para " + account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§a.");
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                StringUtils.formatString(String.format(PartyMessages.PARTY_LEADER_TRANSFERRED, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL)))
            ));
        });
    }

    public void leaveParty(Player player) {
        var playerName = player.getName();
        var party = findPartyPlayer(playerName);

        if (party == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (party.leader().name().equalsIgnoreCase(playerName)) {
            StringUtils.send(player, PartyMessages.PARTY_LEADER_CANNOT_LEAVE);
            return;
        }

        var partyMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(playerName)).findFirst().orElse(null);

        if (partyMember == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(partyMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                log.error("Error fetching player account for leaving party: {}", ex.getMessage());
                return;
            }

            var updatedParty = partyRepository.removeMemberFromParty(party, playerName);
            if (updatedParty == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            player.sendMessage("§cVocê saiu da party.");
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                StringUtils.formatString(String.format(PartyMessages.PARTY_MEMBER_LEFT, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL)))
            ));
        });
    }

    public void deleteParty(Player player) {
        var leaderName = player.getName();
        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (requireLeader(player, party)) {
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(party.leader().uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                sendError(player, "Error fetching player account for deleting party", ex);
                return;
            }

            partyRepository.deleteParty(party);
            StringUtils.send(player, PartyMessages.PARTY_DELETED);
            var message = account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §cdesfez a party.";
            var memberNames = party.members().stream().map(PartyMember::name).toList();
            var memberNamesNotOnlineThisServer = new HashSet<String>();

            memberNames.forEach(memberName -> {
                var targetPlayer = Bukkit.getPlayer(memberName);
                if (targetPlayer != null) {
                    StringUtils.send(targetPlayer, message);
                } else {
                    memberNamesNotOnlineThisServer.add(memberName);
                }
            });

            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, new PartyDeletedPacket(
                memberNamesNotOnlineThisServer.stream().toList(),
                message
            ));
        });
    }

    public void kickParty(String targetName, Player player) {
        var leaderName = player.getName();
        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(leaderName)) {
            StringUtils.send(player, PartyMessages.ONLY_LEADER);
            return;
        }

        var targetMember = party.members().stream().filter(member -> member.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (targetMember == null) {
            StringUtils.send(player, "§cO jogador " + targetName + " §cnão está na sua party.");
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(targetMember.uniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                log.error("Error fetching player account for kicking party member: {}", ex.getMessage());
                return;
            }

            var updatedParty = partyRepository.removeMemberFromParty(party, targetName);
            if (updatedParty == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            player.sendMessage("§eVocê expulsou " + account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §eda party.");
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                StringUtils.formatString(String.format(PartyMessages.PARTY_MEMBER_KICKED, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))),
                true
            ));
        });
    }

    public void sendPartyInfo(Player player) {
        var playerName = player.getName();
        var party = findPartyPlayer(playerName);
        if (party == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        platform.getPlayerAccountService().fetchPlayerAccountList(party.members().stream().map(PartyMember::uniqueId).toList())
            .thenComposeAsync(accounts -> platform.getPlayerAccountService().getPlayerAccount(party.leader().uniqueId())
                .thenAcceptAsync(leaderAcc -> {
                    if (leaderAcc == null) {
                        StringUtils.send(player, PartyMessages.PROCESS_ERROR);
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
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).thenAcceptAsync(senderAccount -> {
            if (senderAccount == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            var formattedMessage = StringUtils.formatString("§d[Party] " + senderAccount.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§f: " + mensagem);
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(), formattedMessage));
        }).exceptionally(ex -> {
            StringUtils.send(player, PartyMessages.PROCESS_ERROR);
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
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (requireLeader(player, party)) {
            return;
        }

        if (party.isOpen()) {
            StringUtils.send(player, PartyMessages.PARTY_ALREADY_OPEN);
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                sendError(player, "Error fetching player account for opening party", ex);
                return;
            }

            var updatedParty = partyRepository.setPartyOpen(party);
            if (updatedParty == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                StringUtils.formatString(String.format(PartyMessages.PARTY_OPENED, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))),
                true
            ));

            for (var onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (party.members().stream().noneMatch(m -> m.name().equalsIgnoreCase(onlinePlayer.getName()))) {
                    StringUtils.send(onlinePlayer, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §etornou a party pública. Utilize o comando /party entrar para entrar em sua party.");
                }
            }
        });
    }

    public void closeParty(Player player) {
        var party = findPartyPlayer(player.getName());
        if (party == null) {
            StringUtils.send(player, PartyMessages.NOT_IN_PARTY);
            return;
        }

        if (requireLeader(player, party)) {
            return;
        }

        if (!party.isOpen()) {
            StringUtils.send(player, PartyMessages.PARTY_ALREADY_CLOSED);
            return;
        }

        platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId()).whenComplete((account, ex) -> {
            if (ex != null) {
                sendError(player, "Error fetching player account for closing party", ex);
                return;
            }

            var updatedParty = partyRepository.setPartyClosed(party);
            if (updatedParty == null) {
                StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                return;
            }

            StringUtils.send(player, "§aVocê fechou a party para novos membros.");
            networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                StringUtils.formatString(String.format(PartyMessages.PARTY_CLOSED_MSG, account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))),
                true
            ));
        });
    }

    public void joinInParty(Player player, String leaderName) {
        if (isPlayerInParty(player.getName())) {
            StringUtils.send(player, PartyMessages.ALREADY_IN_PARTY);
            return;
        }

        var party = findPartyPlayer(leaderName);
        if (party == null) {
            StringUtils.send(player, "§cO jogador " + leaderName + " §cnão está liderando nenhuma party.");
            return;
        }

        if (!party.isOpen()) {
            StringUtils.send(player, PartyMessages.PARTY_CLOSED);
            return;
        }

        var leaderAccount = platform.getPlayerAccountService().getPlayerAccountByName(leaderName);
        var playerAccount = platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId());

        CompletableFuture.allOf(leaderAccount, playerAccount)
            .thenRunAsync(() -> {
                var leaderAcc = leaderAccount.join();
                var playerAcc = playerAccount.join();

                if (leaderAcc == null || playerAcc == null) {
                    StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                    return;
                }

                if (party.members().size() >= getMaxPartySizeForGroup(leaderAcc)) {
                    StringUtils.send(player, PartyMessages.PARTY_FULL);
                    return;
                }

                var partyMember = new PartyMember(player.getName(), playerAcc.uniqueId());
                var updatedParty = partyRepository.addMemberToParty(party, partyMember);

                if (updatedParty == null) {
                    StringUtils.send(player, PartyMessages.PROCESS_ERROR);
                    return;
                }

                StringUtils.send(player, "§aVocê entrou na party de " + leaderName + "§a.");
                networkClient.sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyMessagePacket.create(party.partyId(),
                    StringUtils.formatString(String.format(PartyMessages.PARTY_MEMBER_JOINED, playerAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL))))
                );
            });
    }

    private void validatePartyInviteExists(String senderName, Player player) {
        if (!partyInviteExists(senderName, player.getName())) {
            StringUtils.send(player, String.format(PartyMessages.NO_INVITE, senderName));
        }
    }

    private PartyData processPartyInviteAccept(PartyMember leader, PartyMember receiver) {
        var party = findOrCreateParty(leader);
        return partyRepository.addMemberToParty(party, receiver);
    }

    public PartyData findPartyPlayer(String playerName) {
        return partyRepository.findPartyByPlayer(playerName);
    }

    private PartyData findPartyById(String partyId) {
        return partyRepository.findPartyById(partyId);
    }

    private PartyData findOrCreateParty(PartyMember leader) {
        var party = partyRepository.findPartyByPlayer(leader.name());
        if (party != null) return party;
        return partyRepository.createNewParty(leader);
    }

    public boolean isPlayerInParty(String playerName) {
        return partyRepository.isPlayerInParty(playerName);
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

    private boolean requireLeader(Player player, PartyData party) {
        if (!isLeader(player, party)) {
            StringUtils.send(player, PartyMessages.ONLY_LEADER);
            return true;
        }
        return false;
    }

    private boolean isLeader(Player player, PartyData party) {
        return party == null || !party.leader().name().equalsIgnoreCase(player.getName());
    }

    private void sendError(Player player, String logMessage, Throwable ex) {
        StringUtils.send(player, PartyMessages.PROCESS_ERROR);
        log.error("{}: {}", logMessage, ex.getMessage());
    }
}

package net.warcane.lugin.core.minigames.party;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minigames.party.data.PartyData;
import net.warcane.lugin.core.minigames.party.data.PartyMember;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PartyRepository {
    private static final String REDIS_KEY = "party";
    private static final String EXPIRY_PREFIX = REDIS_KEY + ":exp:";
    private static final String MEMBER_KEY = REDIS_KEY + ":member:";
    private static final String PARTY_KEY_PREFIX = REDIS_KEY + ":party:";

    private final RedisConnector redisConnector;
    private final Cache<String, PartyData> partyIdCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
    private final Cache<String, PartyData> playerNameCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

    public PartyRepository(RedisConnector redisConnector) {
        this.redisConnector = redisConnector;
    }

    private void updatePartyCache(PartyData party, PartyData oldParty) {
        var oldNames = new HashSet<String>();
        oldNames.add(oldParty.leader().name().toLowerCase());
        for (var m : oldParty.members()) {
            oldNames.add(m.name().toLowerCase());
        }
        playerNameCache.invalidateAll(oldNames);

        updatePartyCache(party);
    }

    private void updatePartyCache(PartyData party) {
        partyIdCache.put(party.partyId(), party);
        playerNameCache.put(party.leader().name().toLowerCase(), party);

        for (var m : party.members()) {
            playerNameCache.put(m.name().toLowerCase(), party);
        }
    }

    public void saveParty(PartyData party) {
        redisConnector.useJedis(jedis -> jedis.set(PARTY_KEY_PREFIX + party.partyId(), party.toJson()));
        updatePartyCache(party);
    }

    public PartyData findPartyById(String partyId) {
        var cached = partyIdCache.getIfPresent(partyId);
        if (cached != null) {
            return cached;
        }

        var party = redisConnector.supplyFromJedis(jedis -> {
            var partyData = jedis.get(PARTY_KEY_PREFIX + partyId);
            return partyData != null ? PartyData.fromJson(partyData) : null;
        });

        if (party != null) {
            updatePartyCache(party);
        }

        return party;
    }

    public PartyData findPartyByPlayer(String playerName) {
        var cached = playerNameCache.getIfPresent(playerName.toLowerCase());
        if (cached != null) {
            return cached;
        }

        var party = redisConnector.supplyFromJedis(jedis -> {
            var partyId = jedis.get(MEMBER_KEY + playerName.toLowerCase());

            if (partyId == null) {
                return null;
            }

            return findPartyById(partyId);
        });

        if (party != null) {
            updatePartyCache(party);
        }

        return party;
    }

    public void deleteParty(PartyData party) {
        redisConnector.useJedis(jedis -> {
            var pipeline = jedis.pipelined();
            pipeline.del(PARTY_KEY_PREFIX + party.partyId());
            pipeline.del(MEMBER_KEY + party.leader().name().toLowerCase());
            party.members().forEach(member -> pipeline.del(MEMBER_KEY + member.name().toLowerCase()));
            pipeline.sync();
        });

        partyIdCache.invalidate(party.partyId());
        playerNameCache.invalidate(party.leader().name().toLowerCase());

        party.members().forEach(member -> playerNameCache.invalidate(member.name().toLowerCase()));
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

    public PartyData createNewParty(PartyMember leader) {
        var id = UUID.randomUUID().toString().substring(0, 6);
        var newParty = new PartyData(id, leader, new HashSet<>(), false);

        saveParty(newParty);
        setPlayerParty(leader.name(), newParty.partyId());

        return newParty;
    }

    public void setPlayerParty(String playerName, String partyId) {
        redisConnector.useJedis(jedis -> jedis.set(MEMBER_KEY + playerName.toLowerCase(), partyId));
    }

    public PartyData addMemberToParty(PartyData party, PartyMember member) {
        return redisConnector.supplyFromJedis(jedis -> {
            jedis.watch(PARTY_KEY_PREFIX + party.partyId());

            var partyJson = jedis.get(PARTY_KEY_PREFIX + party.partyId());
            if (partyJson == null) {
                return null;
            }

            var current = PartyData.fromJson(partyJson);
            var updated = current.addMember(member);

            var transaction = jedis.multi();
            transaction.set(PARTY_KEY_PREFIX + party.partyId(), updated.toJson());
            transaction.set(MEMBER_KEY + member.name().toLowerCase(), party.partyId());
            transaction.exec();

            updatePartyCache(updated, current);

            return updated;
        });
    }

    public PartyData removeMemberFromParty(PartyData party, String memberName) {
        return redisConnector.supplyFromJedis(jedis -> {
            jedis.watch(PARTY_KEY_PREFIX + party.partyId());

            var partyJson = jedis.get(PARTY_KEY_PREFIX + party.partyId());
            if (partyJson == null) {
                return null;
            }

            var current = PartyData.fromJson(partyJson);
            var updated = current.removeMember(memberName);
            var transaction = jedis.multi();

            transaction.set(PARTY_KEY_PREFIX + party.partyId(), updated.toJson());
            transaction.del(MEMBER_KEY + memberName.toLowerCase());
            transaction.exec();

            updatePartyCache(updated, current);

            return updated;
        });
    }

    public PartyData transferLeadership(PartyData party, PartyMember newLeader) {
        return redisConnector.supplyFromJedis(jedis -> {
            jedis.watch(PARTY_KEY_PREFIX + party.partyId());

            var partyJson = jedis.get(PARTY_KEY_PREFIX + party.partyId());
            if (partyJson == null) {
                return null;
            }

            var current = PartyData.fromJson(partyJson);
            var updated = current.removeMember(newLeader.name()).addMember(current.leader()).setLeader(newLeader);
            var transaction = jedis.multi();

            transaction.set(PARTY_KEY_PREFIX + party.partyId(), updated.toJson());
            transaction.exec();

            updatePartyCache(updated, current);

            return updated;
        });
    }

    private PartyData updatePartyStatus(PartyData party, boolean open) {
        return redisConnector.supplyFromJedis(jedis -> {
            jedis.watch(PARTY_KEY_PREFIX + party.partyId());

            var partyJson = jedis.get(PARTY_KEY_PREFIX + party.partyId());
            if (partyJson == null) {
                return null;
            }

            var current = PartyData.fromJson(partyJson);
            var updated = open ? current.setOpen() : current.setClose();
            var transaction = jedis.multi();

            transaction.set(PARTY_KEY_PREFIX + party.partyId(), updated.toJson());
            transaction.exec();

            updatePartyCache(updated, current);

            return updated;
        });
    }

    public PartyData setPartyOpen(PartyData party) {
        return updatePartyStatus(party, true);
    }

    public PartyData setPartyClosed(PartyData party) {
        return updatePartyStatus(party, false);
    }

    public boolean isPlayerInParty(String playerName) {
        if (playerNameCache.getIfPresent(playerName.toLowerCase()) != null) {
            return true;
        }

        return redisConnector.supplyFromJedis(jedis -> jedis.exists(MEMBER_KEY + playerName.toLowerCase()));
    }

    public void handleLeaderQuit(PartyData party) {
        redisConnector.useJedis(jedis -> {
            var warnKey = EXPIRY_PREFIX + "offline_warn:" + party.partyId();
            var disbandKey = EXPIRY_PREFIX + "offline:" + party.partyId();
            var transaction = jedis.multi();
            transaction.setex(disbandKey, 4 * 60, "");
            transaction.setex(warnKey, 2 * 60, "");
            transaction.exec();
        });
    }

    public void handleLeaderJoin(PartyData party) {
        redisConnector.useJedis(jedis -> {
            var warnKey = EXPIRY_PREFIX + "offline_warn:" + party.partyId();
            var disbandKey = EXPIRY_PREFIX + "offline:" + party.partyId();
            var transaction = jedis.multi();
            transaction.del(warnKey);
            transaction.del(disbandKey);
            transaction.exec();
        });
    }
}

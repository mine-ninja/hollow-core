package net.warcane.lugin.core.minigames.party;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minigames.party.data.PartyData;
import net.warcane.lugin.core.minigames.party.data.PartyMember;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Repository responsible for all party persistence and cache operations using Redis and Caffeine.
 * Provides atomic, efficient, and consistent access to party data.
 */
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

    /**
     * Updates the local cache for a party and all its members.
     * @param party The party to cache.
     */
    private void updatePartyCache(PartyData party) {
        partyIdCache.put(party.partyId(), party);
        playerNameCache.put(party.leader().name().toLowerCase(), party);

        for (PartyMember m : party.members()) {
            playerNameCache.put(m.name().toLowerCase(), party);
        }
    }

    /**
     * Persists a party in Redis and updates the local cache.
     * @param party The party to save.
     */
    public void saveParty(PartyData party) {
        redisConnector.useJedis(jedis -> jedis.set(PARTY_KEY_PREFIX + party.partyId(), party.toJson()));
        updatePartyCache(party);
    }

    /**
     * Finds a party by its ID, using cache if available.
     * @param partyId The party ID.
     * @return The PartyData or null if not found.
     */
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

    /**
     * Finds a party by a player's name, using cache if available.
     * @param playerName The player's name.
     * @return The PartyData or null if not found.
     */
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

    /**
     * Deletes a party and all its member references from Redis and cache using pipelining.
     * @param party The party to delete.
     */
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

    /**
     * Creates a party invite with a 60-second expiry in Redis.
     * @param senderName The sender's name.
     * @param receiverName The receiver's name.
     */
    public void createPartyInvite(String senderName, String receiverName) {
        redisConnector.useJedis(jedis -> {
            var inviteExpiryKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            jedis.setex(inviteExpiryKey, 60, "");
        });
    }

    /**
     * Removes a party invite from Redis.
     * @param senderName The sender's name.
     * @param receiverName The receiver's name.
     */
    public void removePartyInvite(String senderName, String receiverName) {
        redisConnector.useJedis(jedis -> {
            var inviteKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            jedis.del(inviteKey);
        });
    }

    /**
     * Checks if a party invite exists in Redis.
     * @param senderName The sender's name.
     * @param receiverName The receiver's name.
     * @return True if the invite exists, false otherwise.
     */
    public boolean partyInviteExists(String senderName, String receiverName) {
        return redisConnector.supplyFromJedis(jedis -> {
            var inviteKey = EXPIRY_PREFIX + "invite:" + senderName.toLowerCase() + ":" + receiverName.toLowerCase();
            return jedis.exists(inviteKey);
        });
    }

    /**
     * Creates a new party with the given leader, persists it, and sets the leader's party reference.
     * @param leader The party leader.
     * @return The created PartyData.
     */
    public PartyData createNewParty(PartyMember leader) {
        var id = UUID.randomUUID().toString().substring(0, 6);
        var newParty = new PartyData(id, leader, new HashSet<>(), false);

        saveParty(newParty);
        setPlayerParty(leader.name(), newParty.partyId());

        return newParty;
    }

    /**
     * Sets the party reference for a player in Redis.
     * @param playerName The player's name.
     * @param partyId The party ID.
     */
    public void setPlayerParty(String playerName, String partyId) {
        redisConnector.useJedis(jedis -> jedis.set(MEMBER_KEY + playerName.toLowerCase(), partyId));
    }

    /**
     * Adds a member to a party atomically, updates Redis and cache.
     * @param party The party to update.
     * @param member The member to add.
     * @return The updated PartyData, or null if the party does not exist.
     */
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

            updatePartyCache(updated);

            return updated;
        });
    }

    /**
     * Removes a member from a party atomically, updates Redis and cache.
     * @param party The party to update.
     * @param memberName The member's name to remove.
     * @return The updated PartyData, or null if the party does not exist.
     */
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

            updatePartyCache(updated);
            playerNameCache.invalidate(memberName.toLowerCase());

            return updated;
        });
    }

    /**
     * Transfers party leadership atomically, updates Redis and cache.
     * @param party The party to update.
     * @param newLeader The new leader.
     * @return The updated PartyData, or null if the party does not exist.
     */
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
            updatePartyCache(updated);
            playerNameCache.put(newLeader.name().toLowerCase(), updated);

            return updated;
        });
    }

    /**
     * Sets the party as open or closed atomically, updates Redis and cache.
     * @param party The party to update.
     * @param open True to set open, false to set closed.
     * @return The updated PartyData, or null if the party does not exist.
     */
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

            updatePartyCache(updated);

            return updated;
        });
    }

    /**
     * Sets the party as open (public).
     * @param party The party to update.
     * @return The updated PartyData, or null if the party does not exist.
     */
    public PartyData setPartyOpen(PartyData party) {
        return updatePartyStatus(party, true);
    }

    /**
     * Sets the party as closed (private).
     * @param party The party to update.
     * @return The updated PartyData, or null if the party does not exist.
     */
    public PartyData setPartyClosed(PartyData party) {
        return updatePartyStatus(party, false);
    }

    /**
     * Checks if a player is in a party, using cache first then Redis.
     * @param playerName The player's name.
     * @return True if the player is in a party, false otherwise.
     */
    public boolean isPlayerInParty(String playerName) {
        if (playerNameCache.getIfPresent(playerName.toLowerCase()) != null) {
            return true;
        }

        return redisConnector.supplyFromJedis(jedis -> jedis.exists(MEMBER_KEY + playerName.toLowerCase()));
    }
}

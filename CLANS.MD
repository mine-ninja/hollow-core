# TECHNICAL SPECIFICATIONS: CLANSYSTEM (GUI-BASED & MONGODB)

## 1. CORE CONCEPT
A high-performance, menu-driven Clan plugin for monorepos. It utilizes MongoDB for document-based storage and integrates with a local EconomyAPI for all transactions.

---

## 2. INTERFACE DESIGN (GUI-CENTRIC)
The primary interaction is through /clan, which opens the Clan Hub.

### MENU HIERARCHY
* MAIN MENU (/clan):
    * Profile Icon: Shows clan stats (Name, Tag, Leader, Member count).
    * Member Management: List members -> Click to Manage (Kick, Promote, Change Perms).
    * Upgrades Shop: Visual grid of available tiers (Slots, Perks).
    * Settings: Toggle PvP, change clan colors, etc.
* SAFETY LOCK: The Server Owner ID (defined in config) bypasses all checks. Permissions cannot be revoked from this ID, and they cannot be kicked from the clan they own.

---

## 3. DATA SCHEMA (MONGODB)
Clans are stored in a 'clans' collection.
* DOCUMENT STRUCTURE:
    - _id: UUID
    - tag: String (Indexed, Unique)
    - name: String (Indexed, Unique)
    - ownerId: UUID
    - balance: Double
    - settings: { pvpEnabled: Boolean }
    - members: List of { uuid: UUID, permissions: List of Strings }
    - upgrades: { slot_level: Integer, unlocked_features: List of Strings }

---

## 4. ECONOMY & UPGRADES
Uses the internal EconomyAPI for transactions. Costs and scaling are defined in config.yml.
* TRANSACTION FLOW: Check Balance -> EconomyAPI.withdraw() -> Mongo.update().
* CONFIGURABLE UPGRADES: Member slots, Clan Chat access, and PvP Toggle.

---

## 5. PERFORMANCE & OPTIMIZATION
* DRIVER: Use MongoDB Asynchronous Driver to prevent main-thread lag (no server TPS drops).
* CACHING:
    - Map<UUID, Clan> activeClans: Cached clan data for online players.
    - Map<UUID, UUID> playerToClan: Fast lookup for player-to-clan links.
* INDEXING: Unique indexes on 'tag' and 'name' for O(1) search.

---

## 6. PERMISSIONS LOGIC
Permissions are stored per member as a list of nodes:
* CHAT: Send clan messages.
* MANAGE_MEMBERS: Invite/Kick/Promote.
* UPGRADES: Spend clan funds in the shop.
* PVP_CONTROL: Toggle friendly fire.

---

## 7. COMMANDS
* /clan: Opens the Main GUI.
* /cc <message>: Quick shortcut for Clan Chat.
# HollowCore — Plugin Documentation

> A lightweight, debloated alternative to EssentialsX with fully configurable messages, true RGB TabList support via PacketEvents, and clean permission design.

---

## Table of Contents

1. [Philosophy](#philosophy)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Configuration](#configuration)
5. [Messages](#messages)
6. [Commands](#commands)
7. [TabList](#tablist)
8. [Permissions](#permissions)

---

## Philosophy

HollowCore exists because most utility plugins do too much. EssentialsX is powerful, but it carries years of legacy features, opinionated defaults, and messages you can't fully control.

HollowCore is built around three principles:

**Everything is configurable.** Every message the plugin sends to a player is defined in `messages.yml`. Nothing is hardcoded. Colors, wording, placeholders — all of it is yours to change without touching any code.

**No bloat.** Every feature in HollowCore is intentional. There are no economy integrations, no kit systems, no warp lists, no signs. If it isn't in this document, it isn't in the plugin.

**Packets over abstractions.** Where Bukkit's API falls short — particularly around TabList formatting — HollowCore goes directly to the packet level using PacketEvents. This is what enables true RGB and gradient support in tab list headers, footers, prefixes, and suffixes without any workarounds.

---

## Requirements

| Dependency | Version |
|---|---|
| Paper or Spigot | 1.20+ |
| Java | 17+ |
| PacketEvents | 2.x |

---

## Installation

1. Place `HollowCore.jar` and `PacketEvents.jar` in your `plugins/` folder.
2. Start the server. Default configuration files will be generated automatically.
3. Edit `config.yml` and `messages.yml` to your liking.
4. Reload with `/hollowcore reload` or restart the server.

---

## Configuration

HollowCore is configured through two files: `config.yml` for behavior and `messages.yml` for all player-facing text.

**`config.yml`**

```yaml
spawn:
  world: "world"

teleport:
  delay: 3              # seconds before teleporting (0 to disable)
  request-timeout: 60   # seconds before a /tpa request expires
  cancel-on-move: true  # cancels teleport if the player moves during delay

homes:
  default-limit: 1      # fallback limit when no hollow.homes.<n> permission is found

tablist:
  enabled: true
  update-interval-ticks: 20
  header: "<gradient:#ff0000:#ffff00>Welcome to the Server!</gradient>"
  footer: "<gray>Players online: <white>{online}<gray>/<white>{max}"
  prefix: ""
  suffix: " <gray>[<white>{ping}ms<gray>]"
```

---

## Messages

All player-facing messages live in `messages.yml`. Every entry supports the full [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, including RGB hex colors (`<#RRGGBB>`), gradients, hover events, and click events. Legacy `&` codes are also supported.

Placeholders use `{curly_braces}` and are documented inline for each message.

**`messages.yml`**

```yaml
prefix: "<dark_gray>[<aqua>HollowCore<dark_gray>] "

# --- General ---
no-permission: "<red>You don't have permission to use this command."
player-only: "<red>This command can only be used by players."
player-not-found: "<red>Player <white>{player}</white> was not found."
invalid-usage: "<red>Usage: <white>{usage}"
reload-success: "<green>Configuration reloaded successfully."

# --- Spawn ---
spawn-teleported: "<green>You have been teleported to spawn."
spawn-set: "<green>Spawn has been set to your current location."
spawn-not-set: "<red>Spawn has not been set yet. Use /setspawn first."

# --- Teleportation ---
tp-teleported: "<green>You have been teleported to <white>{target}</white>."
tp-teleported-here: "<green><white>{player}</white> has been teleported to you."
tp-delay: "<yellow>Teleporting in <white>{delay}</white> seconds. Don't move!"
tp-cancelled-move: "<red>Teleportation cancelled because you moved."

# --- TPA ---
tpa-sent: "<green>Teleport request sent to <white>{target}</white>."
tpa-received: "<yellow><white>{player}</white> wants to teleport to you. <green><click:run_command:'/tpaccept'>[Accept]</click> <red><click:run_command:'/tpdeny'>[Deny]</click>"
tpa-no-pending: "<red>You have no pending teleport requests."
tpa-accepted: "<green>Teleport request accepted."
tpa-accepted-sender: "<green><white>{player}</white> accepted your teleport request."
tpa-denied: "<red>Teleport request denied."
tpa-denied-sender: "<red><white>{player}</white> denied your teleport request."
tpa-expired: "<red>Your teleport request to <white>{target}</white> has expired."
tpa-already-pending: "<red>You already have a pending request to <white>{target}</white>."

# --- Homes ---
home-set: "<green>Home <white>{name}</white> has been set."
home-teleported: "<green>Teleported to home <white>{name}</white>."
home-deleted: "<green>Home <white>{name}</white> has been deleted."
home-not-found: "<red>Home <white>{name}</white> does not exist."
home-limit-reached: "<red>You have reached your home limit of <white>{limit}</white>."
home-list: "<green>Your homes: <white>{homes}"
home-list-empty: "<red>You have no homes set."

# --- QoL ---
enderchest-opened: "<green>Opening your Ender Chest."
craft-opened: "<green>Opening a crafting table."
```

---

## Commands

### `/spawn`
Teleports the player to the server spawn point.

- **Permission:** `hollow.spawn` *(default: true)*

---

### `/setspawn`
Sets the server spawn to the sender's current location and facing direction.

- **Permission:** `hollow.setspawn` *(default: op)*

---

### `/tp <player>` · `/tp <player> <target>`
With one argument, teleports yourself to `<player>`. With two arguments, teleports `<player>` to `<target>` — intended for admin use.

- **Permission:** `hollow.tp` · `hollow.tp.others` *(default: op)*

---

### `/tphere <player>`
Teleports the specified player to your location.

- **Permission:** `hollow.tphere` *(default: op)*

---

### `/tpa <player>`
Sends a teleport request to another player. The request expires after the time configured in `teleport.request-timeout`. Only one outgoing request per player is allowed at a time.

- **Permission:** `hollow.tpa` *(default: true)*

---

### `/tpaccept` · `/tpyes`
Accepts the most recent incoming teleport request. If `teleport.delay` is set, the requesting player will be held in place for that many seconds before being teleported. Moving during this window cancels the teleport if `cancel-on-move` is enabled.

- **Permission:** `hollow.tpa` *(default: true)*

---

### `/tpdeny`
Denies the most recent incoming teleport request.

- **Permission:** `hollow.tpa` *(default: true)*

---

### `/home [name]` · `/homes` · `/sethome [name]` · `/delhome <name>`
The full homes suite. Players set named homes at their current position and teleport back to them at any time. If no name is provided, `home` is used as the default.

The number of homes a player can own is determined by the highest `hollow.homes.<number>` permission granted to them — either directly or through a group. If no such permission is found, the plugin falls back to `homes.default-limit` in `config.yml`.

```
hollow.homes.1  → 1 home
hollow.homes.5  → 5 homes
hollow.homes.20 → 20 homes
```

This model means home limits require no additional configuration — just assign the right permission to a rank in your permissions plugin and HollowCore resolves the highest value automatically at runtime.

- **Permissions:** `hollow.home` · `hollow.sethome` · `hollow.delhome` *(default: true)*

---

### `/enderchest` · `/ec`
Opens the player's personal Ender Chest from anywhere, without needing a physical block.

- **Permission:** `hollow.enderchest` *(default: op)*

---

### `/craft`
Opens a virtual crafting table, without needing a physical block.

- **Permission:** `hollow.craft` *(default: true)*

---

## TabList

HollowCore manages the tab list entirely through PacketEvents, sending raw `WrapperPlayServerPlayerListHeaderAndFooter` and `WrapperPlayServerPlayerInfo` packets directly to each player. This bypasses Bukkit's display name limitations entirely and enables full MiniMessage formatting — including RGB colors, gradients, and per-player dynamic values — in all four TabList fields: header, footer, prefix, and suffix.

The tab list is refreshed on a configurable tick interval. All fields support the following placeholders:

| Placeholder | Description |
|---|---|
| `{player}` | Player's name |
| `{online}` | Current online player count |
| `{max}` | Server max player count |
| `{ping}` | Player's ping in milliseconds |
| `{world}` | Player's current world name |

If LuckPerms is present on the server, prefix and suffix are read from the player's active group metadata and applied per-player, allowing rank-specific tab formatting without additional configuration. The global `config.yml` values act as the fallback when no LuckPerms data is found.

**Example configuration:**

```yaml
header: "<gradient:#ff6b6b:#ffd93d:#6bcb77>My Awesome Server</gradient>"
footer: "<#aaaaaa>discord.gg/myserver"
prefix: "<bold><#ff4757>[VIP]</bold> "
suffix: " <#57606f>(<#ff6b6b>{ping}ms<#57606f>)"
```

---

## Permissions

| Node | Description | Default |
|---|---|---|
| `hollow.spawn` | Use `/spawn` | `true` |
| `hollow.setspawn` | Use `/setspawn` | `op` |
| `hollow.tp` | Use `/tp` on yourself | `op` |
| `hollow.tp.others` | Teleport other players with `/tp` | `op` |
| `hollow.tphere` | Use `/tphere` | `op` |
| `hollow.tpa` | Use `/tpa`, `/tpaccept`, `/tpdeny` | `true` |
| `hollow.home` | Use `/home` and `/homes` | `true` |
| `hollow.sethome` | Use `/sethome` | `true` |
| `hollow.delhome` | Use `/delhome` | `true` |
| `hollow.homes.<number>` | Sets the player's home limit to `<number>` | — |
| `hollow.enderchest` | Use `/enderchest` and `/ec` | `op` |
| `hollow.craft` | Use `/craft` | `true` |
| `hollow.reload` | Use `/hollowcore reload` | `op` |
| `hollow.*` | All permissions | `op` |

---

*HollowCore — Less is more.*
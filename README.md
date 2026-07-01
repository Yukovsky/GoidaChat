<div align="center">

# GoidaChat

[![Latest Release](https://img.shields.io/github/v/release/Yukovsky/GoidaChat?style=flat-square&label=latest&color=brightgreen)](https://github.com/Yukovsky/GoidaChat/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue?style=flat-square)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.228+-orange?style=flat-square)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-Apache--2.0-lightgrey?style=flat-square)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/Yukovsky/GoidaChat/build.yml?style=flat-square)](https://github.com/Yukovsky/GoidaChat/actions)

**Server-side chat manager for NeoForge 1.21.1 — routing, moderation, and auto-mod in one mod.**

Local and global chat, private messages, smart mentions, anti-spam, auto-moderation, escalating mutes, and ban/kick/mute with IP and HWID tracking. Built to stay off the main thread under 50+ concurrent players.

| Links | |
|---|---|
| Issues | [github.com/Yukovsky/GoidaChat/issues](https://github.com/Yukovsky/GoidaChat/issues) |
| Releases | [github.com/Yukovsky/GoidaChat/releases](https://github.com/Yukovsky/GoidaChat/releases) |

</div>

---

## Features

### Chat routing

- **Local chat** (default) — radius-based, same dimension only; sender is notified if no one was in range
- **Global chat** — triggered by a leading `!`; color-coded by sender dimension
- **Admin chat** (`/ac`) — visible only to staff with the chat permission
- **Private messages** — `/msg`, `/tell`, `/w`, `/r`; click a player's name in chat to auto-fill `/msg <name>`
- **Social Spy** — staff can opt in to see all private messages server-wide
- **Smart mentions** — exact nickname match, no `@` required; mentioned name is highlighted, with a configurable sound players can mute for themselves

### Moderation

- `/mute`, `/ban`, `/kick` with timed or permanent durations, silent (`-s`) and voice-mute (`-v`) flags
- Bans track UUID, IP, and HWID together under one ban record
- Automatic re-ban of fresh accounts joining from a banned IP/HWID, with staff notified on evasion attempts
- Trusted-account exemptions for legitimate alts
- Ban screen shows reason, time remaining, and an appeal contact pulled from config
- Community `/votemute` and `/voteunmute` — only registered when [GoidaVote](https://github.com/goidacraft) is installed

### Auto-moderation

- Flood, duplicate-message, and character-repeat spam detection (`goidachat.antispam.bypass` exempts staff)
- Advertising filter with a domain whitelist
- Caps-lock ratio check — lowercase-and-pass or block
- Optional profanity wordlist — mask or block
- **Escalation** — repeated violations step through configurable mute durations (warning → 5m → 30m → 1d → 7d by default), resetting after a quiet period

### Logging

- Every chat type logged to disk with configurable retention, pruned automatically on server start

### Integrations *(all optional, auto-detected)*

| Mod | What GoidaChat does with it |
|---|---|
| [LuckPerms](https://luckperms.net/) | Permission nodes and chat prefix/suffix; falls back to vanilla OP levels if absent |
| [FTB Ranks](https://www.feed-the-beast.com/) | Stacks prefix/suffix from all of a player's ranks, accessed via reflection — no hard dependency |
| [HWID (Anti-Alts)](https://modrinth.com/mod/hwid) | Hardware-ID ban and ban-evasion detection |
| [GoidaVote](https://github.com/goidacraft) | Powers `/votemute` and `/voteunmute` |
| [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) | Voice channel is silenced alongside text on a `-v` mute |

### API for other mods

`com.goidacraft.goidachat.api.MuteApi` exposes a stable surface for muting/unmuting players programmatically — guard calls with `ModList.isLoaded("goidachat")`:

```java
MuteApi.mute(uuid, name, reason, expiresAt, mutedBy, voice);
MuteApi.mutePermanent(uuid, name, reason, mutedBy, voice);
MuteApi.unmute(uuid);
MuteApi.unmuteIfBy(uuid, mutedBy);   // only removes if issued by the same source
MuteApi.isMutedBy(uuid, mutedBy);
```

---

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228 or later |
| Java | 21 |
| Side | Server only |

GoidaChat replaces vanilla `/msg`, `/tell`, `/w`, `/teammsg`, `/tm`, `/ban`, `/ban-ip`, `/banlist`, `/pardon`, `/pardon-ip`, and `/kick` with its own implementations.

---

## Installation

1. Download the latest jar from [Releases](https://github.com/Yukovsky/GoidaChat/releases).
2. Place it in the `mods/` folder of your NeoForge server.
3. Start the server — config is created at `config/goidachat-server.toml`.

If you're migrating from the legacy GoidaChat **plugin**, data files (`bans.json`, `mutes.json`, `ignores.json`, etc.) are copied automatically into the mod's config folder on first boot. Already-migrated files are never overwritten.

---

## Configuration

`config/goidachat-server.toml` — created on first launch.

```toml
[chat]
  localRadius = 50                     # blocks
  globalPrefix = "!"
  localFormat = "&8[&aL&8] &r%prefix%%player%%suffix% &e>&r %message%"
  globalFormat = "&8[&aG&8] &r%prefix%%player%%suffix% &e>&r %message%"
  adminFormat = "&8[&cA&8] &r%prefix%%player%%suffix% &e>&r %message%"
  mentionSound = "minecraft:entity.experience_orb.pickup"
  noOneHeard = "&7No one is around to hear you."

[moderation]
  enableIpBan = true
  enableHwidBan = true                 # requires the HWID mod
  hwidFolder = "config/hwid"
  banEvadeNotify = true
  autoBanEvader = true
  appealContact = "discord.gg/example"

[antispam]
  windowMs = 2000
  maxMessages = 3
  duplicateCheckCount = 3              # 0 = off
  charRepeatEnabled = true
  charRepeatMax = 4                    # 0 = off

[automod.advertising]
  enabled = true
  whitelist = ["example.com"]

[automod.caps]
  enabled = true
  threshold = 0.70
  minLength = 8
  action = "LOWERCASE"                 # LOWERCASE | BLOCK

[automod.profanity]
  enabled = false
  action = "REPLACE"                   # REPLACE | BLOCK
  words = []

[escalation]
  violationResetHours = 24
  muteDurations = ["5m", "30m", "1d", "7d"]
  notifyStaff = true

[logging]
  retentionDays = 7
```

---

## Commands

| Command | Permission (OP fallback) | Purpose |
|---|---|---|
| `/msg`, `/tell`, `/w` `<player> <text>` | — | Send a private message |
| `/r <text>` | — | Reply to the last sender |
| `/ignore <player> [pm\|all]` | — | Block PMs or everything from a player (default `pm`) |
| `/unignore <player>` | — | Remove an ignore |
| `/togglemention` | — | Mute your own mention sound |
| `/socialspy` | `goidachat.socialspy` (2) | Toggle seeing other players' PMs |
| `/ac <text>` | `goidachat.adminchat` (2) | Send/see admin-only chat |
| `/adminrest full` | `goidachat.adminrest` (3) | Toggle staff-rest mode, blocking global chat for non-staff |
| `/mute <player> <duration> [-s] [-v] <reason>` | `goidachat.mute` (2) | Timed/permanent mute, optional silent and voice flags |
| `/unmute <player>` | `goidachat.mute` (2) | Remove a mute |
| `/ban <player> <duration> [-s] <reason>` | `goidachat.ban` (3) | Ban by UUID, with optional IP/HWID |
| `/unban`, `/pardon <player>` | `goidachat.ban` (3) | Remove a ban |
| `/kick <player> <reason>` | `goidachat.kick` (2) | Kick an online player |
| `/trustaccount`, `/untrustaccount <player>` | `goidachat.ban` (3) | Exempt/un-exempt an alt from auto ban-evasion |
| `/banlist [player] [info]` | `goidachat.ban` (3) | List or inspect bans |
| `/banlog` | `goidachat.banlog` (3) | View ban-evasion join attempts |
| `/violations list\|info\|add\|edit\|delete\|clear` | `goidachat.violations` (3) | Manage auto-mod violation records |
| `/automod reload\|status` | `goidachat.automod` (3) | Reload or inspect auto-mod state |
| `/votemute`, `/voteunmute` | `goidachat.votemute` | Community vote-mute (requires GoidaVote) |

Duration format: `10m`, `1h`, `2d`, `permanent`.

---

## Building from Source

```bash
git clone https://github.com/Yukovsky/GoidaChat.git
cd GoidaChat
./gradlew build
```

Output: `build/libs/goidachat-<version>.jar`. Requires Java 21.

---

## License

[Apache License 2.0](LICENSE)

# statflex

**Hypixel stats viewer mod for Minecraft Forge 1.8.9**

Minecraft mod that displays Hypixel player stats in-game. It supports Bedwars, Skywars, and Duels, along with a suite of utility features including a denicker, anticheat detector, AutoGG, Discord RPC, and more.

> This mod is **NOT** officially allowed by Hypixel. Use at your own risk.

---

## Features

### Stats Viewer

| Game | Command | Description |
|------|---------|-------------|
| Bedwars | `/s bw [Player] -[Mode]` | Displays stars, finals, and FKDR |
| Skywars | `/s sw [Player] -[Mode]` | Displays stars, wins, and KDR |
| Duels | `/s duels [Player] -[Mode]` | Displays title, wins, and WLR |

- Supports mode-specific lookups (e.g., `-solo`, `-duos`, `-bridge`, `-uhc`)

### Auto Stats List

- **Bedwars** — Automatically lists all players' stats when you run `/who` in a Bedwars game
- **Skywars** — Automatically lists stats when teams are announced in a Skywars game
- **Duels** — Automatically fetches opponent stats when a Duels match starts

- Optional: `/s keepwho` to keep original `/who` output visible alongside the stats list

### Denick

Detects and reveals nicked players. When a nicked player is found, their real name and stats are shown in chat.

> Denick may not be legitimate in Hypixel.

### Anticheat

A client-side anticheat detector that flags nearby players for:

- Auto Block
- NoFall
- NoSlow
- Scaffold / Legit Scaffold

Configurable flag interval (0–20 seconds) to control alert frequency.

### AutoGG

Automatically sends customizable GG messages when a game ends.

- Add/remove messages via `/s autogg [Message]` or the GUI
- Supports multiple messages sent sequentially

### Name History

Look up a player's username history via the Crafty.gg API:

```
/s nh [Player]
```

### Skin Downloader

Download any player's skin as a PNG file:

```
/s skin [Player]
/s skin [Player] -npcskin
```

- `-npcskin` forces saving the skin even if the player is nicked/NPC-skinned
- Configurable save directory via `/s dir [Path]` or the GUI

### Discord Rich Presence

Displays your Minecraft activity as a Discord Rich Presence status via IPC.

- Configurable Application ID
- Auto-connects and updates periodically

### In-Game Update System

Checks GitHub Releases for new versions and provides a one-click update flow that replaces the mod automatically after Minecraft closes.

---

## GUI

Open the settings GUI with `/s`. The GUI provides tabbed configuration:

| Tab | Contents |
|-----|----------|
| General | Denick, Secure Connection, AutoGG messages, Anticheat interval, Discord RPC |
| Bedwars | Auto Stats, Keep /who, Warn thresholds (Level / FKDR) |
| Skywars | Auto Stats, Keep /who |
| Duels | Auto Stats, Updated Titles |
| Hypixel API | API Key input |
| Skin | Download skins, Set download path |
| System | GUI color customization |
| Update | Check and install updates |

---

## Commands

| Command | Description                      |
|---------|----------------------------------|
| `/s` | Open the settings GUI            |
| `/s api [Key]` | Set Hypixel API key              |
| `/s bw [Player] -[Mode]` | Bedwars stats lookup             |
| `/s sw [Player] -[Mode]` | Skywars stats lookup             |
| `/s duels [Player] -[Mode]` | Duels stats lookup               |
| `/s nh [Player]` | Name history lookup              |
| `/s skin [Player] [-npcskin]` | Download player skin             |
| `/s dir [Path]` | Set skin save directory          |
| `/s autogg [Message]` | Manage AutoGG messages           |
| `/s flag [Seconds]` | Set anticheat flag interval      |
| `/s warn [Level] [FKDR]` | Set warn thresholds              |
| `/s list` | Toggle Bedwars stats list        |
| `/s auto` | Toggle Duels auto stats          |
| `/s denick` | Toggle Denick                    |
| `/s rpc` | Toggle Discord RPC               |
| `/s keepwho` | Toggle original /who visibility  |
| `/s secure` | Toggle SSL certificate validation |
| `/s setting` | Show clickable setting in chat   |
| `/s update` | Open Update tab in GUI           |
| `/s help` | Show help in chat                |

---

## Requirements

- Minecraft **1.8.9**
- Forge **11.15.1.2318**
- Java 8+ (To build by yourself)
- A Hypixel API key (Generate at [Hypixel Developer Dashboard](https://developer.hypixel.net))
---

## Installation

### Automatic installation

1. Launch Minecraft 1.8.9 (Badlion Client is not supported).
2. Get statflex injector at [Latest statflex release](https://github.com/Konayukiw/statflex/releases/latest).
3. Just run injector.

### Manual installation

1. Install Minecraft Forge 1.8.9.
2. Get statflex jar at [Latest statflex release](https://github.com/Konayukiw/statflex/releases/latest).
3. Place the jar in your `.minecraft/mods/` folder.
4. Launch Minecraft and register API key via `/s api [Key]` to enable stats viewer.

---

## Building from Source

```bash
gradlew build
```

The output Jar will be in `build/libs/`.

To produce a fatJar with all dependencies bundled:

```bash
gradlew fatJar
```

---

## Tech Stack

- Java 8
- MinecraftForge 1.8.9 (ForgeGradle 2.1)
- SpongePowered Mixin 0.7.11
- Gson 2.8.9
- Hypixel API v2
- Crafty API
- Discord IPC

---

## Disclaimer

This mod is a personal project and is **not endorsed or approved by Hypixel**. Features such as Denick and Auto Stats may be considered unfair advantages. Use responsibly and at your own risk.
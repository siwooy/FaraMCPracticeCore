# FaraMCPracticeCore (open-sourced)

This project was originally a private project for the server **FaraMC**. We've pushed literal boundaries of [StrikePractice](https://www.spigotmc.org/resources/strikepractice-%E2%80%93-1v1-2v2-bots-fights-pvp-events-parties-build-fights-and-more.46906/) and StrikePracticeAPI ([Repo](https://github.com/toppev/StrikePracticeAPI) | [Docs](https://strikepractice-api.toppe.dev/doc/ga/strikepractice/api/StrikePracticeAPI.html)) during this project. However, operating the server has been a real nightmare and I've open-sourced the major parts of the practice server. Enjoy.

> [!NOTE]
> This project is not yet finished and will be finished eventually.

> Learn more at https://dev.siwoo.lol/FaraMCPracticeCore

## Features

- **Arena cloning** — every fight gets its own fresh copy of the arena. Schematics are pasted asynchronously with FastAsyncWorldEdit into dedicated void worlds (`pasteArena1`–`pasteArena3`), so build fights never leave scars on a shared map. When the fight ends, the region is cleared and the slot is recycled for the next fight.
- **Dynamic StrikePractice arenas** — the plugin manages `dynamic` / `dynamicbuild` SP arenas automatically, allocating extra copies (`dynamic_2`, `dynamicbuild_3`, …) when concurrent fights need them and removing them afterwards.
- **Arena selector GUI** — players with permission can pick the arena they queue on.
- **Game modes** — Bed Fight, Fireball Fight, Boxing, Wind Fight, RBW FFA.
- **Training** — practice modes with Citizens-backed training NPCs.
- **PvP bots** — bot duels with queueing and fight-end handling.
- **Parties, duels, ranked/unranked queues** — GUI flows built on top of StrikePractice.
- **Quality-of-life** — kit editor, lobby flight, custom messages, Discord webhooks, an AI coach, and various mechanics fixes (e.g. potion throw).

## How arena cloning works

1. On fight start (`FightStartEvent` / `BotDuelStartEvent`), the plugin picks an `ArenaConfig` for the kit — either the player's GUI selection or a random eligible arena.
2. A paste **slot** is allocated in one of the void paste worlds (slots freed by finished fights are reused; new slots are spaced 5000 blocks apart).
3. The arena schematic is pasted asynchronously via FAWE. Players are teleport-locked until the paste finishes, then teleported to the configured spawn points.
4. On fight end, the pasted region is cleared, chunk tickets are released, and the slot returns to the free pool. If a fight ends while its paste is still running, the clear waits for the paste to land first.
5. If a paste fails (missing schematic, unknown format, WorldEdit error), the session is torn down instead of dropping players into the void.

Arena definitions live in `plugins/FaraMCPracticeCore/arena/<name>.yml` next to their `.schem` files:

```yaml
name: myarena
schematic: myarena.schem        # defaults to <name>.schem
pos1:                           # spawn point 1, relative to center
  ==: Vector
  x: 10.0
  y: 5.0
  z: 0.0
pos2:                           # spawn point 2, relative to center
  ==: Vector
  x: -10.0
  y: 5.0
  z: 0.0
corner1:                        # clear-region corner, relative to center
  ==: Vector
  x: 30.0
  y: 30.0
  z: 30.0
corner2:                        # opposite clear-region corner
  ==: Vector
  x: -30.0
  y: 0.0
  z: -30.0
center:                         # paste offset from the slot center
  ==: Vector
  x: 0.0
  y: 0.0
  z: 0.0
kits: [ "bedfight", "boxing" ]  # empty/omitted = allowed for all kits
```

The in-game setup command (`/faraarena <pos1|pos2|corner1|corner2|center> <mapName>`) writes these values for you from where you're standing.

## Requirements

- **Java 21+** (JVM 22 recommended)
- **Paper** 1.21.11
- Plugins:
  - [StrikePractice](https://www.spigotmc.org/resources/strikepractice-%E2%80%93-1v1-2v2-bots-fights-pvp-events-parties-build-fights-and-more.46906/) (paid, but very crucial)
  - FastAsyncWorldEdit
  - Citizens
  - ProtocolLib
  - PlaceholderAPI

## Configuration

`plugins/FaraMCPracticeCore/config.yml`:

- `discord.status-webhook-url` — Discord webhook for server up/down messages (empty = disabled). Keep this URL secret and out of version control.
- `status-check.url` — optional remote status endpoint checked at startup; if it returns `disable`, the plugin disables itself. Empty = skipped. Network errors are ignored (fail-open).

## Building

Maven project; the StrikePractice API jar is bundled under `libs/`.

```bash
mvn clean package
```

The shaded jar is produced at `target/FaraMCPracticeCore-<version>.jar` — drop it into your server's `plugins/` folder.

## License

By modifying/utilizing this repository, you agree to the terms and license: [Apache License 2.0](https://github.com/siwoolol/FaraMCPracticeCore/blob/master/LICENSE)

Major contributions: [@siwoolol](https://github.com/siwoolol)

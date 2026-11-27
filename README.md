# CyberNpc

**Current development version: 0.45.12** · Minecraft **1.20.1** · Forge **47.4.10** · Java **17**

The living NPC, dialogue and first-arrival layer of **Fractured Island / CyberSpectra Season 2**. Install matching CyberNpc JARs on the client and server.

## Current features

- **Main Seven:** Kai (stable story ID `mason`), Katie, Elara, Rowan, Felix, Nadia and Orin have individual spawn eggs, authored skins, branching personal dialogue, voice blips, per-player Trust/Affection and defensive combat. Their secret personal quests use CyberQuest; six new quests accompany Katie's original Yarn quest. Trust 2 and Affection 1 unlock the personal quest offer; ordinary interaction does not automatically start a quest.
- **Dialogue:** restored split-corner speaker-swap layout, option pagination, voiced/typewritten replies, NPC facing, and Close/ESC recovery for empty or delayed replies. Pope and Guild Receptionist have authored story/service dialogue.
- **Wild NPCs:** biome-appropriate natural expeditions and camps, racial identity, varied class/gear rolls, combat and survival behaviours. CyberRaces owns race choices; CyberClasses owns classes; CyberServer can control starting NPC progression. No natural End expeditions.
- **Zombies:** custom generated appearances with Steve/Alex clothing overlays and a Minecraft-style zombification icon. Living NPC skins support optional Skin Layers 3D integration.
- **Player HUD:** compact top-left static face preview, Minecraft-style health, hunger and armour icons, with optional Iron's Spells 'n Spellbooks mana display.
- **Town services:** guards, Pope, Guild Receptionist, banker, courier and shopkeeper roles. Earlier automatic town housing, employment and guard-post systems are parked for now; old registry IDs and saved data are retained. Older housing/workstation instructions are historical, not the current recommended setup.

## Main Seven personal quest finish (0.45.10)

The seven friendship quests now have individual offers, more personal dialogue and completion follow-ups. Existing quest IDs and objectives remain unchanged. Katie's eligible request is directly accessible, and Kai's keepsake hand-in uses the scoped delivery API in CyberQuest **0.3.0-alpha.24**, preventing that dialogue action from completing or consuming the item for the older Mason quest. The five other quests each include a specific two-choice personal discussion, not a generic research button. Completion does not trigger departure. See [the final personal quest guide](docs/MAIN_SEVEN_PERSONAL_QUESTS.md).

## Friendship testing (0.45.12)

Operators can unlock personal quest offers for a test player with `/cyber npc friendship @s all` or `/cyber npc friendship @s kai`. The default raises Trust to at least 2 and Affection to at least 1 without lowering existing higher values. Supply both numbers for exact values, e.g. `/cyber npc friendship @s all 10 10` (range -10 to 10). This clears the dialogue cooldown for the targeted characters, without starting, completing, resetting or rewarding quests. Reopen dialogue after running it. CyberQuest must be installed to offer the quests. Original `/cybernpc friendship` prefix works too.

## First arrival and guild registration

When both arrival markers are set, new players finish race creation, wait in the one-at-a-time summoning queue, and experience the **13-second** first-arrival cinematic:

1. 0–3.5 seconds: original Fractured Island Blockbench title.
2. 3.5 seconds: first summoning pulse.
3. 8.2 seconds: second pulse.
4. 10.6 seconds: character reveal.
5. 13 seconds: handoff to the Pope's greeting.

The original Photon effect `cyberserver:summoning` is bundled in CyberNpc; the old CyberServer resource-pack ZIP is no longer required. Camera safety, skip and recovery behaviour are retained. With the configured intro, class selection happens later at the Guild Receptionist, with optional one-time starter equipment. There is no physical town-exit gate. CyberQuest supplies newcomer guidance and CyberRaces gates Cyber XP until class registration.

Set the arrival and waiting positions as an operator:

```mcfunction
/cyber npc intro setarrival
/cyber npc intro setpopewait
/cyber arrival test
```

The last command requires CyberServer. See [intro timings](docs/EXTENDED_SUMMONING_INTRO.md) and [bundled effects](docs/INTRO_EFFECTS_BUNDLED.md).

## Shops, banking and mail

Shopkeepers have individual configurable Buy/Sell catalogues. Prices use physical copper-equivalent coins, with physical change and server-side inventory/range checks. New catalogues begin empty; entries are repeatable offers rather than finite warehouse stock.

```mcfunction
/cyber balance
/cyber npc shop list <target>
/cyber npc shop addbuy <target> <priceInCopper> <bundleCount>
/cyber npc shop addsell <target> <priceInCopper> <bundleCount>
/cyber npc shop removebuy <target> <index>
/cyber npc shop removesell <target> <index>
```

Hold the example item in your main hand when adding stock. See [shopkeeper configuration](docs/SHOPKEEPERS.md).

Physical letters travel through Drop Boxes, courier NPCs and player letterboxes. The optional Discord Post Office runs **inside CyberServer 0.2.7+** on the dedicated server. Discord users write letters via a `/mailpanel` embed; the CyberNpc importer validates and deduplicates queued requests. Bind a loaded physical Drop Box while looking at it:

```mcfunction
/cyber npc discordmail bind
/cyber npc discordmail status
/cyber npc discordmail unbind
/cyber server discordmail setup
```

Discord credentials belong only in the server's private `config/cyberserver/discord-mail.properties` or environment. The retained standalone Node bot is a legacy alternative, not needed alongside the embedded bot. Follow [CyberServer's current setup guide](https://github.com/Cyberspectraa/CyberServer/blob/development-no-actions/docs/DISCORD_POST_OFFICE.md).

## Island Rift and series finale

The existing two-block Photon Rift is now an opt-in story ending. Walking through it opens a confirmation; declining leaves the character unchanged. Accepting starts a **72-second** scrolling farewell with the supplied soundtrack, real gameplay statistics, Main Seven relationship recap and acknowledgements. A completed departure disconnects the player and prevents normal dedicated-server re-entry. Interrupted credits replay on return. This is not a portal to another dimension.

Operators can reset their own departure for testing or authorise offline UUID recovery. Use a backed-up test world:

```mcfunction
/cyber npc rift reset
/cyber npc rift recover <uuid>
```

See [Rift finale and recovery](docs/RIFT_FINALE.md).

### NPC departures and developer ledger (0.45.11)

Operators can arm a Main Seven NPC for a deliberate crossing. An authorised NPC must intersect an intact Rift; no automatic departures are linked to friendship quests. The world-wide Departure Ledger shows completed player and named NPC departures. Entity snapshots are saved on the server for testing recovery. Use `/give @s cybernpc:rift_departure_ledger` or `/cyber npc rift ledger`. See [NPC crossing and recovery instructions](docs/RIFT_DEPARTURE_LEDGER.md).

## Unified commands (0.45.9)

CyberNpc provides the shared `/cyber` command hub. It links to the original Brigadier commands of installed mods, retaining their arguments and permissions. Original prefixes still work.

| Section | Original command |
| --- | --- |
| `/cyber npc` | `/cybernpc` |
| `/cyber quest` | `/cyberquest` |
| `/cyber races` | `/cyberraces` |
| `/cyber classes` | `/cyberclasses` |
| `/cyber server` | `/cyberserver` |
| `/cyber arrival` | `/arrival` |
| `/cyber reset` | `/cyberresetall` (destructive test reset) |
| `/cyber balance` | `/bal` (now registered) |

`/cyber help <section>` lists available subcommands; `/cyber doctor` is an operator-only registration check, not a gameplay test. Other operator examples:

```mcfunction
/cyber npc spawn <name>
/cyber npc mason spawn
/cyber npc remove <target>
/cyber npc admin role <target> <role>
/cyber npc admin wander <target> <true|false>
/cyber npc admin hunger <target> <0..20>
```

See [complete command hub guide](docs/UNIFIED_COMMAND_HUB.md).

## Companion mods

| Mod | Development version | Responsibility |
| --- | --- | --- |
| [CyberRaces](https://github.com/Cyberspectraa/CyberRaces/tree/development-no-actions) | 0.3.2-alpha.76 | Race creation, abilities, Cyber Level |
| [CyberClasses](https://github.com/Cyberspectraa/CyberClasses/tree/development-no-actions) | 0.2.1-alpha.13 | Classes, equipment/magic, Class Books |
| [CyberQuest](https://github.com/Cyberspectraa/CyberQuest/tree/development-no-actions) | 0.3.0-alpha.25 | Journal, quests, contracts and guild |
| [CyberServer](https://github.com/Cyberspectraa/CyberServer/tree/development-no-actions) | 0.2.7 | Server world policy, arrival and Discord bot |

Photon is required for the Rift. Other integrations include optional Pehkui, Better Combat, Iron's Spells 'n Spellbooks and supported equipment/cosmetic mods. Install according to the modpack's dependency requirements.

## Local Windows build

Use Git and JDK 17. Do not run GitHub Actions or modify main.

```powershell
cd "$HOME\CyberMods\CyberNpc"
git switch development-no-actions
git pull --ff-only
.\build-local.bat
```

First checkout: `git clone --branch development-no-actions https://github.com/Cyberspectraa/CyberNpc.git` from the CyberMods directory. Replace the old JAR on both sides only after a successful local build. Back up test worlds.

## Documentation and status

- [Main Seven personal quests](docs/MAIN_SEVEN_SECRET_QUESTS_AND_ZOMBIE_ICON.md)
- [Current dialogue layout and recovery](docs/SPLIT_DIALOGUE_REPLY_RESTORE.md)
- [NPC skins](docs/NAMED_NPC_SKINS.md)
- [Guild Receptionist](docs/GUILD_RECEPTIONIST_DIALOGUE.md)
- [Pope sanctuary dialogue](docs/FRACTURED_ISLAND_POPE_SANCTUARY.md)
- [Local build guide](docs/BUILD_LOCALLY.md)
- [Previous README and historical development notes](https://github.com/Cyberspectraa/CyberNpc/blob/9d20ba5d463e64755b3c1f778e7693205d5ced75/README.md)

This is a development/test build, not a finished release. Older documents may describe superseded behaviour. Source updates do not establish successful compilation or multiplayer gameplay; verify the current versions locally before production use.

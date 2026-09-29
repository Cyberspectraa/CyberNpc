# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.3.0

Current functionality:

- One persistent humanoid CyberNpc entity shared by all NPC types.
- Minecraft's built-in Steve appearance as the default NPC skin.
- Persistent NPC types: Main, Quest, and Wild.
- Separate Main, Quest, and Wild preset spawn eggs.
- Main and Quest eggs can be renamed in an anvil before use to give the spawned NPC that custom name.
- Wild NPCs receive generated names when they do not already have a custom name.
- Wild NPCs can spawn naturally in the Overworld at a deliberately low spawn weight.
- Main and Quest NPCs do not naturally spawn.
- Wild NPCs have a persistent aggression level.
- Repeatedly hurting a Wild NPC builds provocation; higher-aggression NPCs become hostile sooner.
- A Wild NPC that becomes hostile alerts nearby Wild NPCs within 20 blocks.
- Alerted NPCs join the fight without recursively alerting more NPCs.
- Wild NPCs disengage after their target stays more than 40 blocks away for 5 seconds.
- Wild NPCs spawn with a melee weapon selected from the `cybernpc:wild_npc_weapons` item tag.
- Wild NPCs can use their equipped weapon in melee combat.
- Optional YDM's Weapon Master compatibility: when `weaponmaster_ydm` is installed, passive Wild NPC weapons are rendered stowed on the body; during combat they move into the NPC's hand.
- NPC name and role data save with the world.
- Per-NPC wandering toggle.
- Right-click NPC information.
- Admin commands for spawning, roles, wandering, and removal.
- Automatic GitHub Actions build validation.

### Commands

- `/cybernpc spawn <name>`
- `/cybernpc role <target> <role>`
- `/cybernpc wander <target> <true|false>`
- `/cybernpc remove <target>`

NPCs made with `/cybernpc spawn` are Main NPCs.

Targets can use normal Minecraft entity selectors, for example:

`@e[type=cybernpc:cyber_npc,sort=nearest,limit=1]`

## NPC types

CyberNpc uses one entity with stored type data instead of separate mob implementations.

- **Main**: manually placed important NPC.
- **Quest**: manually placed NPC reserved for quest behaviour.
- **Wild**: ordinary NPC intended for natural world spawning.

No quest system or Main-NPC-specific combat behaviour is added in this release.

## Wild NPC weapon pool

The default weapon pool is defined by the item tag:

`cybernpc:wild_npc_weapons`

The included defaults are wooden/stone/iron swords and wooden/stone axes. A modpack datapack can add compatible modded melee weapons to this tag without changing CyberNpc code.

## YDM's Weapon Master compatibility

CyberNpc does not require YDM's Weapon Master. When the Forge mod with ID `weaponmaster_ydm` is present, CyberNpc adds a body-mounted stowed-weapon render layer for its custom NPC entity so the NPC's weapon is visible on its person while passive. CyberNpc does not patch or replace YDM itself.

## Development rule

New gameplay systems and NPC features are added only when explicitly requested by the modpack owner. The codebase should remain modular so requested features can be added without unnecessary rewrites.

Technical fixes, compatibility work, build tooling, and maintainability changes may be made when required to keep requested functionality working correctly.

## Development target

- Minecraft: 1.20.1
- Loader: Forge
- Forge baseline: 47.4.10
- Java: 17

## Build

CI uses Gradle 8.8 and Java 17:

`gradle build --no-daemon`

The reobfuscated mod JAR is generated under `build/libs/`.

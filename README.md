# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.3.2

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
- Wild NPCs own a melee weapon selected from the `cybernpc:wild_npc_weapons` item tag.
- Passive Wild NPCs visibly carry their stored weapon on a player-style body attachment point.
- The stored weapon and its attachment position are synchronized to clients and persist with the NPC.
- Wild NPCs can use several persistent stow positions across the back/body/hips/legs instead of every NPC using one identical placement.
- When combat starts, the stored weapon disappears from the body and is equipped in the NPC's main hand.
- When combat ends, it returns to the NPC's stored body position.
- The draw/stow hook is reusable by a future food-hunting system; hunting behavior itself is not included yet.
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

## Wild NPC weapon pool

The default weapon pool is defined by the item tag:

`cybernpc:wild_npc_weapons`

The included defaults are wooden/stone/iron swords and wooden/stone axes. A modpack datapack can add compatible modded melee weapons to this tag without changing CyberNpc code.

## YDM's Weapon Master compatibility

YDM's Weapon Master makes player hotbar items visible on the character and supports multiple attachment locations. CyberNpc entities are custom mobs rather than Player entities, so YDM does not automatically render their inventory. CyberNpc therefore synchronizes its stored weapon and renders it on player-model attachment points while passive, using attachment styles based on YDM's default player configuration. When the NPC enters combat, the stored-body render is removed and the weapon is rendered in the hand instead.

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

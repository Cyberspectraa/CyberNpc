# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.2.0

Current functionality:

- One persistent humanoid CyberNpc entity shared by all NPC types.
- Minecraft's built-in Steve appearance as the default NPC skin.
- Persistent NPC types: Main, Quest, and Wild.
- Separate Main, Quest, and Wild preset spawn eggs.
- Main and Quest eggs can be renamed in an anvil before use to give the spawned NPC that custom name.
- Wild NPCs receive generated names when they do not already have a custom name.
- Wild NPCs can spawn naturally in the Overworld at a deliberately low spawn weight.
- Main and Quest NPCs do not naturally spawn.
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

The type is a foundation only. No quest system or Main-NPC-specific behaviour is added yet.

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

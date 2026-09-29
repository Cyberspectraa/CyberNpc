# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.1.1

Current functionality:

- Persistent humanoid NPC entity.
- Minecraft's built-in Steve appearance as the default NPC skin.
- Spawn egg.
- Lightweight idle AI.
- NPC name and role data that save with the world.
- Per-NPC wandering toggle.
- Right-click NPC information.
- Admin commands for spawning, roles, wandering, and removal.
- Automatic GitHub Actions build validation.

### Commands

- `/cybernpc spawn <name>`
- `/cybernpc role <target> <role>`
- `/cybernpc wander <target> <true|false>`
- `/cybernpc remove <target>`

Targets can use normal Minecraft entity selectors, for example:

`@e[type=cybernpc:cyber_npc,sort=nearest,limit=1]`

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

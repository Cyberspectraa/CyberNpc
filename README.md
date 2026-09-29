# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built specifically to grow with the CyberSpectra modpack without becoming a fragile collection of one-off NPC scripts.

## Current release: 0.1.0 Foundation

The first release deliberately focuses on a stable core:

- Custom persistent humanoid NPC entity.
- Custom CyberNpc skin/renderer.
- Spawn egg.
- Lightweight idle AI.
- NPC name and role data that save with the world.
- Per-NPC wandering toggle.
- Right-click NPC information.
- Admin commands for spawning, roles, wandering, and removal.
- Automatic GitHub Actions build on every pushed version.

### Commands

- `/cybernpc spawn <name>`
- `/cybernpc role <target> <role>`
- `/cybernpc wander <target> <true|false>`
- `/cybernpc remove <target>`

Targets can use normal Minecraft entity selectors, for example:

`@e[type=cybernpc:cyber_npc,sort=nearest,limit=1]`

## Design direction

CyberNpc will be expanded in layers rather than trying to ship everything at once. Planned systems include an in-game NPC editor, homes and town locations, schedules, Sims-style needs, jobs, dialogue, shops, relationships, quest/event hooks, custom appearances, animations, and modpack integration APIs.

The important rule is that those systems build on persistent NPC data and modular components instead of hard-coding special behavior into individual NPCs.

## Development target

- Minecraft: 1.20.1
- Loader: Forge
- Forge baseline: 47.4.10
- Java: 17

## Build

CI uses Gradle 8.8 and Java 17:

`gradle build --no-daemon`

The reobfuscated mod JAR is generated under `build/libs/`.

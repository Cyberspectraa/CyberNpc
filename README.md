# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.4.0

Current functionality:

- One persistent humanoid CyberNpc entity shared by all NPC types.
- Minecraft's built-in Steve appearance as the default NPC skin.
- Persistent NPC types: Main, Quest, and Wild.
- Separate Main, Quest, and Wild preset spawn eggs.
- Main and Quest eggs can be renamed in an anvil before use to give the spawned NPC that custom name.
- Wild NPCs receive generated names when they do not already have a custom name.
- Wild NPCs can spawn naturally in the Overworld at a deliberately low spawn weight.
- Main and Quest NPCs do not naturally spawn.
- Wild NPCs have a persistent aggression level and can call nearby Wild NPCs for help against players.
- Wild NPCs disengage after a distant target remains out of range.
- Wild NPCs now own exactly two combat weapons: one sword plus one bow or crossbow.
- Weapons stay hidden while passive and are only equipped while fighting or hunting.
- Wild NPC combat switches between the ranged weapon at distance and the sword at close range.
- Wild NPCs have a persistent hunger value from 0 to 20.
- Hunger slowly decreases over time. At 0 hunger, the NPC takes starvation damage and can die.
- Right-clicking a Wild NPC shows its hunger as a 10-segment bar.
- Hungry Wild NPCs search for adult prey from the `cybernpc:wild_npc_prey` entity tag.
- Before hunting, the NPC compares the prey's current/max health with its own health and only attacks prey it considers manageable.
- The default prey list is cow, pig, chicken, sheep, and rabbit.
- After a successful hunt, the NPC searches the kill area for dropped raw food and physically walks over to collect it.
- Raw food comes from the `cybernpc:wild_npc_raw_food` item tag.
- NPCs carrying raw food search for a campfire, soul campfire, furnace, or smoker within 12 blocks.
- At a cooking point, the NPC spends 5 seconds cooking one food item and immediately eats it to restore hunger.
- If no prey, dropped food, or cooking point can be found, hunger continues dropping and the NPC can eventually starve.
- Added `/cybernpc hunger <target> <0-20>` for testing and administration.
- The earlier YDM/body-mounted weapon rendering experiment has been removed.

### Commands

- `/cybernpc spawn <name>`
- `/cybernpc role <target> <role>`
- `/cybernpc wander <target> <true|false>`
- `/cybernpc hunger <target> <0-20>`
- `/cybernpc remove <target>`

NPCs made with `/cybernpc spawn` are Main NPCs.

Targets can use normal Minecraft entity selectors, for example:

`@e[type=cybernpc:cyber_npc,sort=nearest,limit=1]`

## Wild NPC weapon pools

Wild NPCs have exactly two weapon slots:

- `cybernpc:wild_npc_swords`: wooden, stone, and iron swords by default.
- `cybernpc:wild_npc_ranged_weapons`: bow and crossbow by default.

A modpack datapack can extend those tags later without changing the NPC AI.

## Wild NPC hunger and hunting

The hunger system uses a 0-20 scale. Wild NPCs begin hunting at 12 hunger or below. Hunger decreases by one point every 60 seconds. At zero hunger they take starvation damage every four seconds.

Hunting is limited to the `cybernpc:wild_npc_prey` tag. A Wild NPC will not hunt babies and will refuse prey whose health looks too dangerous relative to its own.

After killing prey, it searches nearby dropped items tagged `cybernpc:wild_npc_raw_food`. Once food is collected, the NPC searches locally for a campfire, soul campfire, furnace, or smoker, walks there, cooks food, and eats until its hunger reaches at least 18 or it runs out of food.

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

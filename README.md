# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.5.0

### Wild NPC AI

Wild NPCs now have a more player-like combat and movement layer:

- Exactly two owned weapons: one sword plus one bow or crossbow.
- Weapons remain hidden while the NPC is passive.
- Bow users visibly draw the bow before firing.
- Crossbow users visibly charge, hold a charged crossbow, then fire.
- Crossbow state is synchronized to clients so the player model uses the correct charge/hold pose.
- Ranged aim predicts target movement and compensates for projectile drop based on distance.
- Bow draw strength changes with target distance.
- Ranged accuracy changes with target distance.
- NPCs close distance if a target is too far away or line of sight is blocked.
- NPCs switch to their sword at close range.
- Hostile NPCs sprint while closing for melee.
- Hunting NPCs crouch/sneak while stalking prey and approach more slowly before attacking.
- Wild NPC navigation can swim, open/pass wooden doors, jump normal terrain, and climb climbable blocks when their path reaches them.
- The existing aggression, nearby-help, hunger, hunting, food collection, cooking, eating, and starvation systems remain active.
- The experimental YDM/body-mounted weapon renderer remains removed.

### Player-like capability rule

CyberNpc is intended to move and interact more like a player where an autonomous NPC can make a sensible decision. Building and mining are intentionally excluded.

This release establishes player-like locomotion and weapon use: walking, sprinting, sneaking, swimming, jumping, door use, climbable-block movement, combat weapon switching, ranged aiming, collecting hunted food, cooking, and eating.

Other player activities that require their own decisions or systems can be added to this same AI foundation without turning the NPC into a fake Player entity.

### Commands

- `/cybernpc spawn <name>`
- `/cybernpc role <target> <role>`
- `/cybernpc wander <target> <true|false>`
- `/cybernpc hunger <target> <0-20>`
- `/cybernpc remove <target>`

NPCs made with `/cybernpc spawn` are Main NPCs.

## Development target

- Minecraft: 1.20.1
- Loader: Forge
- Forge baseline: 47.4.10
- Java: 17

## Development rule

New gameplay systems and NPC features are added only when explicitly requested by the modpack owner. Building and mining remain excluded from player-like Wild NPC behavior.

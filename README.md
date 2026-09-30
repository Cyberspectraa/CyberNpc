# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.7.0

### Wild NPC combat

- Wild NPCs own one sword plus one bow or crossbow.
- Bow users visibly draw before firing.
- Crossbow users visibly charge, hold, and fire.
- Ranged aim predicts target movement and compensates for projectile drop.
- NPCs switch to their sword at close range.
- **Sword attacks now play the normal main-hand swing animation on the same tick as the melee hit.**
- Wild NPCs can call nearby Wild NPCs for help when player aggression crosses their personal threshold.
- Hunting NPCs sneak while stalking prey.

### Hunger, hunting, cooking, and corrals

- Persistent hunger from 0 to 20.
- Hungry NPCs hunt manageable adult prey and never deliberately hunt babies.
- Killed prey is collected as real dropped food.
- Raw food is inserted into real furnaces/smokers or placed on real lit campfires.
- NPCs wait for Minecraft's real cooking process, retrieve the cooked result, hold it, and eat it.
- Existing fenced corrals can be detected and managed without allowing NPC building/mining.
- NPCs can lead in a breeding pair, breed them, preserve at least two adult breeders, and hunt surplus adults.

### Bed claiming and sleep

Wild NPCs can now establish a persistent personal bed:

- At night, a Wild NPC that is not fighting and is not hungry enough to need immediate hunting can look for a nearby bed.
- The selected bed position is saved with that NPC and reused on later nights.
- Loaded nearby Wild NPCs will not deliberately claim the same bed.
- The NPC physically walks to its claimed bed and uses Minecraft's real sleeping state and bed orientation.
- The bed's vanilla occupied flag is only set while the NPC is actually asleep; the persistent claim itself is stored by CyberNpc.
- The NPC wakes when daytime arrives, if its bed is removed, if combat starts, or if it takes damage.
- Hunger continues to function while sleeping, so an NPC that becomes too hungry can eventually be forced back into its survival loop.

### Player-like movement

Wild NPCs can walk, sprint, sneak, swim, jump normal terrain, use wooden doors, traverse climbable blocks, use weapons, collect food, cook, eat, manage livestock, and sleep.

Building and mining remain intentionally excluded.

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

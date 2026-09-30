# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.11.0

### Emergency healing

Wild NPCs now treat low health as an emergency instead of continuing a normal damage trade.

- At 8 health or lower, an NPC prioritizes retreating from an active hostile.
- Once it has created enough distance, it stops and visibly eats a carried edible item.
- Food consumption now uses Minecraft's real item-finish path, so special foods apply their actual effects.
- Golden apples therefore apply their vanilla regeneration/absorption effects.
- Emergency eating also restores a small amount of immediate health based on the food's nutrition so ordinary food can help recovery too.
- Food pulled from a storage chest is ranked so high-value recovery items such as golden apples are preferred over ordinary food.

### Full inventory drops on death

Wild NPC deaths now drop their complete internal inventory instead of only normal mob equipment chances.

Guaranteed carried drops include:

- Stored sword.
- Stored bow/crossbow.
- Raw food.
- Ready-to-eat food.
- Food currently being eaten.
- Any actual equipped item still present.

The weapon copy used only for rendering/combat is stowed before death so it cannot duplicate the stored weapon.

### Zombification

A new harmful effect named **Zombification** has been added.

- Duration: exactly **2 minutes (2400 ticks)**.
- Each successful zombie-family melee hit has a **1%** chance to infect a Wild NPC if it is not already infected.
- An infected NPC is no longer treated as a valid target by hostile mobs, and nearby hostiles already targeting it are cleared.
- The effect displays normal status-effect particles/icon.
- When the timer reaches its end, the NPC converts into a Zombie NPC.
- Nearby healthy Wild NPCs periodically notice infected NPCs. The closer the infection is to completion, the higher the chance they become suspicious and move away.
- Developer Glasses report the remaining zombification time while inspecting an infected NPC.

### Zombie NPCs

Zombie NPCs are real Zombie-derived entities rather than Wild NPCs pretending to be zombies.

- They inherit normal vanilla zombie AI, sounds, movement, daylight burning, combat behavior, drowning conversion, reinforcement behavior, and player/villager targeting.
- They use the vanilla zombie appearance, matching the zombie version of the current Steve-based Wild NPC skin.
- They use the normal vanilla zombie loot table.
- Their original NPC name is preserved through conversion.
- Zombie NPC attacks can apply Zombification because they are true Zombie subclasses.

There is also a separate **5%** chance that a Wild NPC killed directly by a zombie-family mob immediately rises as a Zombie NPC. The original NPC still drops its full inventory first, and all of its bed/pen/cooking claims are released.

### Existing systems

CyberNpc also retains improved sword spacing/group combat, retreat AI, food chest storage, hunger/starvation, real cooking, full-pen ownership, livestock breeding/harvesting, safe leash transfer and gate realignment, persistent beds, player-style animations, Developer Glasses, and natural Wild NPC spawning.

Building and mining remain intentionally excluded.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

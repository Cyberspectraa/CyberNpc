# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.10.0

### Smarter hostile-mob combat

Wild NPCs no longer simply run straight into a hostile and trade hits.

- Sword combat now keeps practical spacing instead of standing inside zombies and similar mobs.
- NPCs attack on a faster player-like rhythm, then back away/strafe during the recovery window.
- If they get too close to the hostile they create space before committing to the next hit.
- Ranged weapons remain preferred outside close sword range.
- Low-health NPCs can ask nearby Wild NPCs for help.
- NPCs that decide a hostile is too dangerous call nearby helpers before giving up and fleeing.
- Up to three nearby healthy Wild NPCs can join the same fight.
- At critically low health the NPC retreats instead of suicidally remaining in combat.
- Creepers remain treated as a retreat threat.

### Movement cleanup

- Fleeing NPCs no longer crouch-run.
- Sprint state is automatically cleared if the NPC is not actually moving or its path has ended.
- This prevents the visual bug where an NPC appears to run in place and continuously creates sprint particles while standing still.

### Food storage

Wild NPCs can now use ordinary nearby chests as a food reserve.

- Surplus raw meat left after the NPC has eaten enough can be deposited into a chest.
- Surplus ready-to-eat food can also be stored.
- Any edible crop/food item the NPC is already carrying can use the same storage path, so future crop harvesting feeds into this system automatically.
- A hungry NPC with no carried food checks nearby chests before immediately hunting.
- Ready food is preferred; raw stored food is retrieved and sent through the existing cooking system.
- The NPC can carry a small batch back out of the chest for later consumption.

### Pen harvesting and recovery

The v0.9.1 whole-pen claim/lead-safety system remains in place, with additional livestock behaviour:

- If a claimed herd has produced a baby and there are at least two adults, a hungry NPC may harvest exactly one adult for food.
- A long cooldown prevents the NPC from immediately killing the remaining adult while the baby is still growing.
- Babies are never selected as the harvest target.
- If a leashed animal stops making progress into the pen for several seconds, the NPC performs a realignment maneuver instead of staying stuck forever.
- It walks back out from the gate, moves away to straighten the leash/animal, then walks back through the gate and retries.
- Only a limited number of realignment attempts are made before the animal is safely abandoned/temporarily blacklisted.

### Claims after death

- Dead NPCs stop all AI and claim-discovery ticks immediately.
- On death the NPC closes/interupts its active livestock task, wakes from bed if needed, and releases its persistent world claims.
- Pen and cooking-station claims therefore become available to other NPCs instead of being reclaimed during the death-removal window.

### Existing systems

CyberNpc also retains persistent bed claiming/sleeping, hostile mob targeting, Enderman gaze reactions, fight-or-flee threat evaluation, adaptive bow/crossbow use, hunger/starvation, real furnace/smoker/campfire cooking, ground-food pickup, persistent cooking-station claims, full-enclosure pen claims, real livestock leads, multi-gate pen routing, player-style animations, Developer Glasses debugging, and natural Wild NPC spawning.

Building and mining remain intentionally excluded.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

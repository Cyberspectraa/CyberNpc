# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.6.0

### Wild NPC survival AI

Wild NPCs keep the combat and player-like movement systems from 0.5.0:

- One sword plus one bow or crossbow.
- Visible bow draw and crossbow charge/hold/fire states.
- Adaptive ranged aiming and target-motion prediction.
- Sword switching at close range.
- Sneaking while stalking prey.
- Swimming, jumping, door use, climbable-block movement, sprinting, food pickup, cooking, and eating.
- Persistent hunger, aggression, hunting, nearby NPC assistance, and starvation.

### Real cooking interaction

Cooking now uses the actual Minecraft block inventories/processes instead of a fake timer.

- After collecting raw meat, the NPC searches for a usable **furnace, smoker, campfire, or soul campfire**.
- Furnaces/smokers are only used if they are already burning or have valid fuel available.
- The NPC physically walks to the cooking block.
- For a furnace/smoker, it inserts one raw food item into the real input slot and waits for the real output slot to produce cooked food.
- For a lit campfire, it places one raw food item into the campfire's real cooking inventory and waits for the cooked item to pop out.
- The NPC then retrieves the cooked food, holds it, performs an eating wait, and restores hunger.
- Pending cooking/eating state is saved with the NPC where possible.

### Corral and livestock system

Wild NPCs do **not** build pens, because building/mining remains excluded.

Instead they can use an existing enclosed pen made from fences/walls with a fence gate:

- A hungry NPC can search for a nearby existing enclosed corral.
- It detects the enclosed side of a fence gate rather than treating every gate as a pen.
- It chooses a breedable prey species already in the pen, or a nearby species where at least two adults are available.
- It holds the correct vanilla breeding/lure food for that animal type.
- It opens the gate, leads missing animals through it, and closes the gate again.
- It maintains at least **two adult breeders**.
- When two suitable adults are present, it puts them into love mode so normal Minecraft breeding can create offspring.
- Babies are protected from hunting.
- If the pen later has at least three adults, a hungry NPC may hunt one surplus adult while preserving the breeding pair.
- At critically low hunger, the NPC stops spending time establishing a herd and falls back to normal hunting so it does not knowingly starve while waiting for livestock.

The initial corral detector is intended for ordinary, mostly level fenced pens. More complicated multi-level enclosures can be expanded later if needed.

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

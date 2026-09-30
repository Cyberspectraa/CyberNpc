# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.12.0

### Proper Wild NPC inventory

Wild NPCs now own a real persistent **18-slot inventory** instead of separate hard-coded sword, ranged-weapon, raw-food, and ready-food variables.

- Sword, bow/crossbow, raw meat, cooked/edible food, golden apples, and future carried items now live in the same inventory.
- Slot positions persist across save/reload.
- Existing v0.11.0 NPCs automatically migrate their old stored weapon/food fields into inventory slots when loaded.
- The first two weapon types are no longer fixed variables: combat searches the inventory for a valid sword and ranged weapon.
- New Wild NPCs still receive their normal starting sword and bow/crossbow, but those are inserted into inventory like ordinary owned items.
- Combat equips a temporary hand copy for rendering/use while the authoritative item remains in the inventory, preventing accidental loss during animation/state changes.
- Death drops every inventory slot at 100%, plus any item currently being consumed and genuine equipped armor/items.

### Food and cooking now use inventory

- Hunting drops and useful ground food are inserted into available inventory slots and can stack naturally.
- NPCs can carry multiple different food types at once rather than one raw stack and one ready stack.
- Cooking searches the inventory for raw food and consumes one item from its real stack when placing it in a furnace/smoker/campfire.
- Hungry NPCs choose among available edible inventory items.
- Ordinary hunger tries to preserve golden/enhanced apples when normal food is available.
- Emergency low-health eating chooses the strongest recovery food in the inventory.
- Food chest storage now transfers food between the NPC inventory and chest slots instead of copying special food variables.
- Retrieved chest food is inserted into available inventory space and respects stack limits.

### Developer Glasses

The Developer Glasses inventory line now reports the real inventory:

- Used slots out of 18.
- Actual carried item names and stack counts.
- The actively-used/eaten item is shown separately while it is temporarily out of the inventory.

### Existing systems

v0.12.0 otherwise keeps the v0.11.0 behavior: emergency healing, zombie/Zombification system, combat spacing and group help, food storage, hunger/starvation, real cooking, persistent beds/cooking claims/pen claims, livestock management, player-style animations, and Developer Glasses.

No farming, social AI, or additional gameplay system has been added in this release.

Building and mining remain intentionally excluded.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

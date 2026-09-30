# Changelog

## 0.12.0

- Added a persistent 18-slot Wild NPC inventory.
- Replaced separate stored sword, ranged weapon, raw food, and ready food fields with inventory item lookup.
- Inventory slot positions persist across saves/reloads.
- Added automatic migration of pre-0.12 weapon and food NBT into the new inventory.
- Starting Wild NPC weapons are inserted into inventory instead of stored in dedicated fields.
- Combat selects sword and ranged weapon from inventory.
- Hunting drops and ground food now use normal inventory stacking/capacity.
- Cooking consumes raw food directly from an inventory stack.
- Food chest storage/retrieval now transfers items to/from the real NPC inventory.
- NPCs can carry multiple food types at once.
- Normal hunger preserves golden apples when ordinary food exists.
- Emergency eating still prioritizes the strongest recovery food.
- Death drops the complete inventory contents.
- Developer Glasses now display used inventory slots and actual stack contents.

## 0.11.0

- Added emergency healing and real food effects.
- Added full carried-inventory drops for the previous storage model.
- Added Zombification and Zombie NPC conversion.

## 0.10.0

- Improved hostile combat, food storage, pen recovery, and claim cleanup.

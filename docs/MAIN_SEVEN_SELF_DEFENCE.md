# Main Seven — individual weapons and close-range self-defence

CyberNpc 0.44.92

Each authored town character now holds a fixed, named melee weapon:
- Kai (`mason`) — iron sword, **Kai's Blade**
- Katie (`town_npc_1`) — stone sword, **Katie's Shortblade**
- Elara (`town_npc_2`) — iron axe, **Elara's Trail Axe**
- Rowan (`town_npc_3`) — stone axe, **Rowan's Hatchet**
- Felix (`town_npc_4`) — golden sword, **Felix's Gilded Sword**
- Nadia (`town_npc_5`) — iron pickaxe, **Nadia's Workshop Pick**
- Orin (`town_npc_6`) — golden axe, **Orin's Keeper Axe**

Only NPCs with these exact persisted story IDs receive equipment/AI.
Existing saved characters receive a weapon only if their main hand is empty.
Administrator-assigned equipment is preserved.

Defence AI only attacks nearby hostile monsters (`Enemy`) when a monster
targets the NPC or approaches within four blocks, with line of sight.
It does not hunt, attack players, target passive animals or use the
Wild NPC combat/party systems. Chases are limited to ten blocks from the
combat start and the NPC attempts to return after the threat is gone.
This does not guarantee survival against bosses, creepers or large raids.

The dialogue menu layouts have NOT changed in this version; five preview
options will be presented for review before any UI modification.

Manual verification:
1. Spawn all seven unique spawn eggs and load an existing saved world.
   Check each character's named weapon appears and persists after reload.
2. Assign an alternative weapon to one character with an admin editor;
   the default weapon must not overwrite it.
3. Spawn one zombie near a character. It should only defend itself when
   close or targeted, swing and damage the hostile monster, then try to
   return to its original position.
4. Confirm no player, peaceful mob or other Main Seven NPC is attacked.
5. Confirm unconfigured NPCs, Wild NPC class combat, quest dialogue,
   the Pope, Guild Receptionist, Wild voices and Rift are unchanged.
6. Test in a dedicated server as well as singleplayer.

Changes committed for local build; no automated compilation or in-game
testing has been performed here.

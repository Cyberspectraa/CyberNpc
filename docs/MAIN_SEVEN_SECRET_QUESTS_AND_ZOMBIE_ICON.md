# The Fractured Island — Seven Secret Quests & Zombification Icon

CyberNpc **0.44.91** (development-no-actions). Requires CyberQuest
**0.3.0-alpha.21** on its corresponding development-no-actions branch for
the six new quest definitions.

## Quests / dialogue

All Main Seven use their **existing identities and existing authored dialogue**.
Players earn Trust and Affection via previous conversation rules. Once a
character has **Trust >= 2 and Affection >= 1**, their personal quest becomes
available. The NPC alone offers it in a hidden dialogue option; ordinary
right-clicks do not automatically add it to the journal. The source of
truth for quest objectives, completion and one-time rewards remains CyberQuest.

| NPC | Quest ID | Objective |
| --- | --- | --- |
| Kai (saved ID `mason`) | `cyberquest:kai_choso_plush` | Show his existing Choso Plush |
| Katie | `cyberquest:katie_yarn` | Existing 64 white yarn crafting quest |
| Elara | `cyberquest:elara_lost_route` | Craft a map, discuss safe routes |
| Rowan | `cyberquest:rowan_welcome_meal` | Craft eight bread, discuss welcoming arrivals |
| Felix | `cyberquest:felix_fair_deck` | Craft twelve paper, discuss the real story behind cards |
| Nadia | `cyberquest:nadia_failsafe` | Craft two repeaters, discuss failsafe testing |
| Orin | `cyberquest:orin_star_notes` | Craft a spyglass, compare observations |

Katie keeps the original `katie_yarn` quest unchanged, **including existing
active/completed state**. Only offering to new players is friendship-gated;
already active players can finish without meeting the threshold.

Kai's plush item `cybernpc:choso_plush` was already implemented with model.
The item is **not naturally spawned**, so the series owner must hide/place it
in a chest or other location manually. Its `DELIVER_ITEM` quest objective
uses `consume:false`, so the player keeps the keepsake. The quest hand-in
explicitly validates the held item server-side (not via client dialogue trust).

Other five new quests have complementary **CRAFT** and **DISCOVER** steps.
Discussion is explicitly validated through the correct NPC's active quest.
Either objective can be done first; a final conversation turns it in.
Completion grants a small vanilla-XP reward through CyberQuest and a single
Trust point through CyberNpc. Repeating a completed choice cannot farm it.

All branching/condition/quest actions are server-authoritative. No new
packets, worldgen, NPC AI ticks, or external mods were added. The seven existing
skin/voice assets and memories remain unchanged.

## Zombie-face status effect icon

Replaced `textures/mob_effect/zombification.png` with an **18x18 nearest-
neighbour pixel-art icon** made directly from the uploaded zombie-face crop.
The 8x8 source face was enlarged to 16x16 and centred with a 1px transparent
border, preserving exact existing Minecraft zombie pixels. No new image
synthesis or AI style transfer was used. The existing effect ID remains.

## Local test (not compiled in this environment)

1. Build **both** updated CyberNpc and CyberQuest JARs locally.
2. Talk to every Main Seven character across game days until Trust >= 2 and
   Affection >= 1. Secret dialogue should be absent beforehand.
3. Test all six new quests: accept, complete craft/discovery or item hand-in,
   finish through dialogue, and verify one-time rewards and follow-up dialogue.
4. Test Kai's held Choso Plush both present and absent; a successful hand-in
   must not remove the plush.
5. Test Katie's previously active `katie_yarn` quest survives unchanged.
6. Test two players independently; new quest choices and memories must not
   leak. Test `/reload` of JSON dialogue and quest scripts.
7. Verify Zombification status effect uses the new face icon on the HUD/inventory.
8. Confirm the Pope, Guild Receptionist, player/NPC voices and cosmetic Rift
   are unchanged.

Build **not** run; only source/data validation and GitHub commit were done.

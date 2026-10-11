# CyberNpc

**Current version: 0.44.40** · Minecraft **1.20.1** · Forge **47.4.10** · Java **17**

CyberNpc supplies the living NPC layer for the CyberSpectra Season 2 modpack: world inhabitants, Wild NPC adventurers, professions, dialogue, combat, households, shops, banking, postal services and the first-arrival introduction.

CyberNpc is a reusable gameplay mod. [CyberRaces](https://github.com/Cyberspectraa/CyberRaces) owns race definitions, [CyberClasses](https://github.com/Cyberspectraa/CyberClasses) owns shared class definitions, [CyberQuest](https://github.com/Cyberspectraa/CyberQuest) owns quests and guild contracts, and [CyberServer](https://github.com/Cyberspectraa/CyberServer) owns this world's progression rules. Optional integrations are checked before use.

## NPC types and daily life

- **Main NPCs:** persistent named residents that can be given homes, beds and workstations.
- **Quest NPCs:** entities usable as quest givers by CyberQuest, including entities with a stable `CyberQuestNpcId`.
- **Wild NPCs:** naturally generated adventurers with random appearances, races (when CyberRaces is available), classes, gear quality, personality, progression and survival behaviour.
- **Special service roles:** Banker, Courier, Guard, Pope, Guild Receptionist and Shopkeeper; dedicated spawn eggs and service AI.
- **Mason:** special named NPC with a matching spawn egg, skin and the Choso Plush storyline supplied through CyberServer/CyberQuest.

NPCs have humanoid models, visual armour/equipment, generated names, speech/reaction bubbles and social responses. Wild NPCs can become friends, form parties, assist allies, respond to threats, flee, ride compatible horses, hunt animals, collect food, cook, eat, sleep and recover. Their 18-slot inventories and important world data are persistent. Environmental and navigation logic includes fence/trapdoor avoidance and stuck-path recovery.

### Natural Wild NPC expedition groups (0.44.42)

Natural Wild NPCs now spawn as **small expeditions of 2–4 in the Overworld** and **2–3 in the Nether** (rare; Forge biome spawner weights 2 and 1 respectively). **No natural CyberNpc Wild NPCs spawn in the End**; spawn eggs and admin-created NPCs continue to work without restrictions. The existing Forge natural spawn caps and spawn checks still apply, so pack size may be smaller if nearby positions are blocked.

Members of the same vanilla natural-spawn pack receive a persistent shared party ID, a leader and seeded friendships. When CyberRaces is installed, their base race is picked using the natural-spawn pack ID and the local habitat, so same-biome expedition members generally belong to the same race. Race evolutions remain **players-only**, and Undead remains excluded from Wild NPCs.

**Spawn frequency (0.44.42):** Overworld biome spawn weight is 4 and Nether weight is 2. In established worlds where the passive-mob cap blocks ordinary spawns, the server also checks for a bounded expedition opportunity every 30 seconds per loaded Overworld/Nether dimension, with a maximum of 12 Wild NPCs in a 96-block radius per player. It searches nearby cave floors for underground players rather than only the surface, and checks neighbouring elevations when assembling packs. This respects `doMobSpawning`, stays away from players, never generates chunks and never creates natural groups in the End. The check is probabilistic, so encounters are not guaranteed every 30 seconds.\n\nThe first member of a natural pack **tries to establish one campfire** a short distance from its arrival position. A camp is placed only in clear air over ordinary naturally occurring ground, away from dangerous nearby structures and other campfires. Existing terrain and blocks are not excavated or replaced. This respects the `mobGriefing` gamerule, avoids the End, and records the placement attempt so an NPC/chunk reload cannot leave additional campfires. Some terrain or protected sites may have no safe camp spot.

Wild NPCs already hunt, store raw food, claim free campfire slots, cook their food and pick up cooked results; this release reuses that AI rather than creating a parallel cooking system. Camps are **stationary** wilderness stops, not carried while roaming. When `mobGriefing=false` camps are not placed, though existing placed campfires remain usable.

### Wild combat and class equipment

CyberNpc executes NPC combat and equipment; CyberClasses defines class identities, rarities, restrictions and Level 20 advancements when installed. Normal **Classless** NPCs remain common. Wild NPCs may have melee, ranged, guard, support or casting behaviour depending on their class. Weapon and armour sets improve with gear quality:

| Gear tier | Generation chance |
| --- | ---: |
| Standard | 70% |
| Fine | 20% |
| Rare | 8% |
| Elite | 2% |

These percentages describe the Wild NPC gear-tier roll, **not** the chance of receiving a particular class. Armour and combat behaviour respond to the assigned class and advancement; naturally spawned levels/advancement gates can additionally be controlled by CyberServer.

With **Iron's Spells 'n Spellbooks** installed, compatible Wild casters use real Iron's spells and spellbooks, school-appropriate armour, casting times and animations. Implemented mage schools include Fire, Ice, Lightning, Nature, Holy, Ender, Blood and Evocation. School-specific spellbooks are used where available; Ice and Lightning can use generic Iron's books. The spellbooks remain in NPC inventories and are not duplicated into Curios. Instant and long-cast spells are supported; continuous/channelled spell casting is not yet supported by the NPC casting bridge.

Other optional combat/equipment bridges include **Better Combat**, **Epic Knights** and **Tinkers' Construct**. **CyberRaces** applies base-race appearances, stats, biome-aware natural race rolls and race-owned AI to Wild NPCs (no Wild NPC evolutions). **Pehkui** is accommodated for scaled NPC visuals and speech bubble positioning.

### Zombie NPCs

CyberNpc includes Wild NPC zombies and a zombification effect. The spawn-replacement handler replaces **exact vanilla `minecraft:zombie` entities** with CyberNpc zombie variants, retaining suitable original entity state. Drowned, Husks and Zombie Villagers are not part of this replacement.

## Towns, buildings and NPC jobs

The current setup uses **four invisible marker blocks**, with small interaction/selection outlines but no normal collision:

| Marker | Purpose |
| --- | --- |
| **Building Marker** | Scans and records a bounded interior building area using the surrounding solid walls. |
| **Room Marker** | Extends the same building record into another room or section. Multiple room markers are supported. |
| **Bed Marker** | Links the nearest usable bed within 8 blocks to that building. Residents can use available beds. |
| **Work Marker** | Records the exact working block and facing direction for an assigned NPC. |

A convenient setup order is **Building → optional Room → Bed → Work**. Room/Bed/Work markers placed before the Building Marker can retry linking when interacted with later. Removing a main Building Marker removes its building record.

Use the **Town Register** to select an NPC, then right-click a Building Marker to link its home and a Work Marker to assign its job. Main NPCs can travel to work during the day and return to their linked home/bed at night; special NPCs can also update their stored home/work anchors. Sneak-right-click marker removal and old-world data migration remain supported.

The previous **Building Planner and legacy marker items** remain registered for save compatibility, but are not the recommended setup method.

### Town services

- **Guards:** guard posts, guard assignment, patrols, bell/alert responses, combat assistance and threat defence.
- **Pope:** church routines, waiting positions and the scripted first-arrival greeting.
- **Banker / currency:** persistent player balances, copper/silver/gold/platinum/dragon coins and banking interactions.
- **Shopkeeper:** basic buying/selling using the coin economy.
- **Courier / mail:** letter paper, addressed/sealed/opened letters, drop boxes, letter boxes, courier satchels and delivery routes.
- **Guild Receptionist:** registration and turn-in of CyberQuest's physical Guild Board contracts when CyberQuest is installed.
- **Developer Glasses:** a tabbed NPC inspector displaying class, gear quality, school, spell and other debug/state information.

## First-arrival introduction

When both locations are configured, CyberNpc coordinates a one-at-a-time first-arrival queue after character creation and class selection. The current cinematic includes:

1. The wooden **TESTING VERSION** Blockbench title as **temporary artwork**, with a title fade.
2. A controlled camera sweep aimed at the exact configured summoning block centre.
3. CyberServer summoning visuals when CyberServer is present, followed by the character reveal.
4. A short closing fade, restoration of the **player's normal camera**, then the Pope's welcome dialogue.

The cinematic is **2.25 seconds of title followed by 6.5 seconds of summoning**, with server-authoritative timing, an ESC skip option and recovery from interruptions. The camera and presentation in **0.44.39** are the accepted baseline; there is no plan to change the animation without a new request.

Set the locations while standing at the required blocks:

```mcfunction
/cybernpc intro setarrival
/cybernpc intro setpopewait
```

With CyberServer installed, test the whole flow using `/arrival test`. With CyberRaces installed, the admin command `/cyberresetall` resets character progression and can re-arm the introduction for testing; **it is destructive to the targeted character's progression**.

## Commands

These CyberNpc commands require permission level 2:

```mcfunction
/cybernpc spawn <name>
/cybernpc mason spawn
/cybernpc remove <target>
/cybernpc intro setarrival
/cybernpc intro setpopewait
/cybernpc admin role <target> <role>
/cybernpc admin wander <target> <true|false>
/cybernpc admin hunger <target> <0..20>
```

The old `/cyberintro` top-level commands and former top-level `/cybernpc role`, `wander` and `hunger` are no longer the current command paths. For server arrival testing, use the CyberServer `/arrival` commands.

## Integration and implementation notes

- Core mod: **CyberNpc**. Compatible gameplay integrations are optional unless your modpack explicitly requires them.
- **CyberRaces** provides race identity/scale and optional race-specific Wild NPC actions; **CyberClasses** owns the common class registry and advancement definitions.
- **Iron's Spells 'n Spellbooks** provides real NPC magic; **Better Combat**, **Epic Knights**, **Tinkers' Construct**, **Better Horses** and **Pehkui** have optional compatibility paths.
- **CyberQuest** integrates NPC quest IDs and guild services; **CyberServer** provides world policy, intro effects and seeded story content.
- Not every class/advancement has unique combat AI, not every third-party modded block has custom pathfinding, and full gameplay/performance validation still requires in-pack testing.

For detailed tuning, see the current Java implementation and the corresponding CyberRaces, CyberClasses, CyberQuest and CyberServer repositories. Do not delete legacy registry IDs or world data when updating existing saves.

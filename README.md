# CyberNpc

**Current version: 0.44.55** · Minecraft **1.20.1** · Forge **47.4.10** · Java **17**

CyberNpc supplies the living NPC layer for the CyberSpectra Season 2 modpack: world inhabitants, Wild NPC adventurers, professions, dialogue, combat, households, shops, banking, postal services and the first-arrival introduction.

CyberNpc is a reusable gameplay mod. [CyberRaces](https://github.com/Cyberspectraa/CyberRaces) owns race definitions, [CyberClasses](https://github.com/Cyberspectraa/CyberClasses) owns shared class definitions, [CyberQuest](https://github.com/Cyberspectraa/CyberQuest) owns quests and guild contracts, and [CyberServer](https://github.com/Cyberspectraa/CyberServer) owns this world's progression rules. Optional integrations are checked before use.

### Developer Glasses removal (0.44.52)

Developer Glasses and the associated tabbed developer HUD have been
removed, along with their creative item, model, texture, translation entries
and client V-key binding. Existing world items referencing
`cybernpc:developer_glasses` will no longer resolve when worlds are loaded.
This does not change NPC AI, data, dialogue, typed-chat speech bubbles, or
the ordinary tools used to register towns and houses. The separate
voice-to-bubble experimental feature remains reverted.

## Workstation removal keybind (0.44.55)

**J — Remove Registered Workstation** is a new configurable key in
Minecraft Controls > Key Binds. Hold the **Town Register in your main
hand**, switch it to **WORK MODE**, look directly at a registered
workstation from within six blocks, then press **J**.

* Only the CyberNpc workplace registration is removed. The actual
  workstation block, its inventory, town homes, and old Work markers
  are untouched.
* Removing an **extra** station leaves the main job and employee intact.
* Removing the **main** workstation of a group promotes an available
  linked station to main and preserves the assigned employee and
  original job. Distant chunks are never force-loaded to do this.
* Removing a single/final workstation dissolves that job registration;
  the employee becomes unemployed and may later be hired elsewhere.
* The removal is validated and ray-traced by the server. The client
  does not get to specify arbitrary target coordinates.
* Removing a station clears the register's current multiselect root,
  so choosing the next main station is explicit. The K multiselect
  switch itself stays on/off as chosen.
* This update advances CyberNpc's network protocol. **Both client and
  server must run CyberNpc 0.44.55**. No CyberServer update is needed.

## Multiple workstations per job (0.44.54)

A single registered town job can now own **up to eight linked workstations**.
This is useful for a forge with a smithing table, anvil, grindstone and furnace,
without hiring four separate blacksmiths.

**How to use:**

1. In the Town Register, enter WORK MODE (sneak + right-click air).
2. With the register in your **main hand**, press **K** (configurable in
   Minecraft Controls) to enable WORKSTATION MULTISELECT.
3. Click the **main workstation** to select it. If it has not been registered,
   it is registered first. Its original job type becomes the job for the group.
4. Click additional supported work blocks within **24 blocks**, in the same
   town, to attach them as activity points. A previously registered empty
   single-station job can be attached; a station with an employee or already
   belonging to a multi-station workplace cannot be stolen.
5. **Sneak-click an extra station** while grouping to unregister/detach it.
   The main station itself cannot be detached this way.
6. Press **K** again to finish. The Town Register remembers normal WORK MODE.
   Assign an employee normally by selecting a housed Main NPC and sneak-clicking
   any workstation in the group, or let an unemployed resident claim the vacancy.

All linked stations share **one job slot and one worker**. The worker
periodically moves between **loaded, valid and walkable** station positions
during their existing work shifts, roughly every 65 seconds. There are no
automatic production/crafting effects yet.

Saved format `cybernpc_town_work_v1` is backward compatible. Any workplace
from 0.44.53 without a `Root` field remains an independent one-station job.
Legacy invisible Work markers and the original housing system are unchanged.
If a main station is broken, the registry attempts to promote a valid loaded
extra while keeping the original job title and worker; other chunks are not
force-loaded. The new key/control packet requires **CyberNpc 0.44.54 on both
server and client**. CyberServer 0.2.4 is still compatible.

## Town workplace and employment prototype (0.44.53)

The first, **opt-in** workplace system works alongside the registered towns
and homes introduced in 0.44.46. It does not overwrite or reinterpret old
building markers, existing home assignments, or special-service NPC workplaces.

**Town Register modes:**

1. Hold the Town Register. **Sneak + right-click the air** to switch between
   HOME and WORK modes. HOME mode retains normal bell/door house registration,
   population toggle and manual house assignments.
2. In WORK mode, **normal-click a vanilla workstation** to register one job
   slot for the nearest registered town bell (within 256 blocks). There must
   be one walkable block beside the station; the system reserves this place
   for NPC pathfinding and displays a short registration/status message.
3. Residents who own a **registered town home** may automatically claim
   vacant workstations in the same town. One nearby, loaded worker at most
   is hired per 60-second check per loaded dimension, within 96 blocks of
   their station; no chunks are loaded by the job system.
4. Optionally select a housed ordinary Main NPC with the Town Register,
   then **sneak-click a registered station in WORK mode** to assign them
   immediately. Sneak-click again with that same selected NPC to unassign
   them; the vacancy may be claimed again by an unemployed resident.
5. Registered stations display a subtle gold particle indicator only to
   nearby players holding the Town Register in WORK mode (at most 8 stations
   per holder every 12 ticks). In HOME mode, registered house doors retain
   their own emerald indicators.

**Initially supported vanilla blocks:** Smithing Table / Blast Furnace /
Grindstone / Anvil (Blacksmith), Lectern (Librarian), Composter (Farmer),
Smoker (Cook), Brewing Stand (Alchemist), Barrel (Merchant), Fletching
Table (Fletcher), Cartography Table (Cartographer), Stonecutter (Mason),
Loom (Tailor), Crafting Table (Artisan) and Furnace (Smelter).

Job assignments are persisted in separate Overworld saved data
`cybernpc_town_work_v1`; existing player home ownership is untouched.
Unloaded residents keep their reserved jobs. Once the original station
block is removed/replaced and its chunk is loaded, its registration is
invalidated on the next employment check. A dead NPC releases their job.

**Prototype daytime behaviour:** new station employees commute to a free
adjacent standing spot during two staggered daily work shifts, with a
midday break, brief breathers, and free-time wandering/social behaviour
between shifts. They return to their registered home at night using the
existing home brain. Existing **Work marker** NPCs remain on their old
exact-position AI (intentionally preserved). This first update does not
simulate production, trades, crafting or multi-station buildings yet.

**Test caveat:** The workstation/job engine is a first in-game test build;
check navigation near doors, enclosed workplaces, daily activity balance,
and saved assignment persistence. Be sure CyberNpc 0.44.53 is installed
on both the server and clients. No CyberServer version change is needed.

## NPC types and daily life

- **Main NPCs:** persistent named residents that can be given homes, beds and workstations.
- **Quest NPCs:** entities usable as quest givers by CyberQuest, including entities with a stable `CyberQuestNpcId`.
- **Wild NPCs:** naturally generated adventurers with random appearances, races (when CyberRaces is available), classes, gear quality, personality, progression and survival behaviour.
- **Special service roles:** Banker, Courier, Guard, Pope, Guild Receptionist and Shopkeeper; dedicated spawn eggs and service AI.
- **Mason:** special named NPC with a matching spawn egg, skin and the Choso Plush storyline supplied through CyberServer/CyberQuest.

NPCs have humanoid models, visual armour/equipment, generated names, speech/reaction bubbles and social responses. **Wild NPCs never open the written NPC dialogue UI**: right-clicking one produces a contextual, non-verbal reaction bubble and its existing grunt/vocalisation. Only Main, Quest and service NPCs can speak through scripted dialogue. This restriction is checked both during entity interaction and by the server dialogue controller to prevent stale sessions or indirect calls. Administrative tools, race abilities, social behaviour and Wild NPC reactions remain compatible. Wild NPCs can become friends, form parties, assist allies, respond to threats, flee, ride compatible horses, hunt animals, collect food, cook, eat, sleep and recover. Their 18-slot inventories and important world data are persistent. Environmental and navigation logic includes fence/trapdoor avoidance and stuck-path recovery.

### Natural Wild NPC expedition groups (0.44.43)

Natural Wild NPCs now spawn as **small expeditions of 2–4 in the Overworld** and **2–3 in the Nether** (rare; Forge biome spawner weights 2 and 1 respectively). **No natural CyberNpc Wild NPCs spawn in the End**; spawn eggs and admin-created NPCs continue to work without restrictions. The existing Forge natural spawn caps and spawn checks still apply, so pack size may be smaller if nearby positions are blocked.

Members of the same vanilla natural-spawn pack receive a persistent shared party ID, a leader and seeded friendships. When CyberRaces is installed, their base race is picked using the natural-spawn pack ID and the local habitat, so same-biome expedition members generally belong to the same race. Race evolutions remain **players-only**, and Undead remains excluded from Wild NPCs.

**Spawn frequency (0.44.43):** Overworld biome spawn weight is 4 and Nether weight is 2. In established worlds where the passive-mob cap blocks ordinary spawns, the server also checks for a bounded expedition opportunity every 30 seconds per loaded Overworld/Nether dimension, with a maximum of 12 Wild NPCs in a 96-block radius per player. It searches nearby cave floors for underground players rather than only the surface, and checks neighbouring elevations when assembling packs. This respects `doMobSpawning`, stays away from players, never generates chunks and never creates natural groups in the End. The check is probabilistic, so encounters are not guaranteed every 30 seconds.\n\n**World-loading safety (0.44.43):** Camps are never constructed from entity-join callbacks during chunk generation. Pack leaders wait until they have ticked in a playable server world before attempting their one-off campfire. Terrain and camp checks operate only on fully loaded chunks, avoiding synchronous worldgen requests. Supplemental spawn scans have a short startup grace and check no more than two players per 30-second pass, rotating across players on multiplayer servers. Natural biome spawning, pack identity, classes, races, campfire safety and End restrictions remain intact.

The first member of a natural pack **tries to establish one campfire** a short distance from its arrival position. A camp is placed only in clear air over ordinary naturally occurring ground, away from dangerous nearby structures and other campfires. Existing terrain and blocks are not excavated or replaced. This respects the `mobGriefing` gamerule, avoids the End, and records the placement attempt so an NPC/chunk reload cannot leave additional campfires. Some terrain or protected sites may have no safe camp spot.

Wild NPCs already hunt, store raw food, claim free campfire slots, cook their food and pick up cooked results; this release reuses that AI rather than creating a parallel cooking system. Camps are **stationary** wilderness stops, not carried while roaming. When `mobGriefing=false` camps are not placed, though existing placed campfires remain usable.

### Wild NPC town-spawn protection (0.44.49)

Wild NPCs **do not spawn naturally inside or near protected towns**. This includes standard Forge biome/pack spawns and the bounded supplemental expedition generator. The exclusion is horizontal (also protects caves and rooftops), persists in the existing town save data, and does not generate/load chunks:

- Within **320 blocks** of a registered Town Register bell (256-block house registration area plus a 64-block wilderness buffer).
- Within **144 blocks** of each registered house door, including houses far from the town bell.
- Within **192 blocks** of the Overworld world spawn point, whether or not a bell town has been established.
- Within **192 blocks** of CyberNpc's configured first-arrival/summoning position in its dimension, when set.

Wild NPC camps will also never be newly constructed within these protected areas, even when a previously spawned Wild NPC wanders into town. **This does not remove or teleport existing Wild NPCs, stop them walking through towns, change the Nether wilderness outside the protected area, or restrict spawn eggs/admin spawns or Main NPC town residents.** No setup changes are required for already registered bells and houses. The constants are in `TownWildSpawnProtection` for future tuning.

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

With **CyberRaces 0.3.2-alpha.47+**, Slime Wild NPCs render their colour as part of the base skin rather than a second translucent animated model. This keeps female slim arms aligned with bow-drawing poses. If CyberRaces is missing or an older release is installed, CyberNpc keeps the original renderer and does not crash. No race, class, NPC inventory or save data changes are required.

Other optional combat/equipment bridges include **Better Combat**, **Epic Knights** and **Tinkers' Construct**. **CyberRaces** applies base-race appearances, stats, biome-aware natural race rolls and race-owned AI to Wild NPCs (no Wild NPC evolutions). **Pehkui** is accommodated for scaled NPC visuals and speech bubble positioning.

### Zombie NPCs

CyberNpc includes Wild NPC zombies and a zombification effect. The spawn-replacement handler replaces **exact vanilla `minecraft:zombie` entities** with CyberNpc zombie variants, retaining suitable original entity state. Drowned, Husks and Zombie Villagers are not part of this replacement.

## Towns, buildings and NPC jobs

### Separate house ownership (0.44.50)

Registering a home must never count other houses in the same terrace, row, apartment building or roofed street. House scanning keeps the **128-block horizontal / 48-block vertical safety limits** but now recognises exterior doors, treats **other registered house entrances as solid ownership boundaries**, and rejects beds nearer a competing exterior entrance than the clicked front door. True interior room doors remain traversable. Both leaves of a double entrance count as one house, rather than creating two homes.

Each real bed can belong to **at most one registered home**. On a new registration, beds accidentally claimed by an older home may be reassigned to the new, nearer front door **only if unoccupied**. Occupied/assigned beds and persistent residents are protected from automatic reassignment; the game reports excluded neighbouring beds. Repeat the Town Register click on an older home to refresh its exclusive beds and correct stale scans. Scans are still on-demand, bounded, and never force chunks to load. If two buildings have no meaningful structural division or use an unusual shared entrance, add a proper dividing doorway/wall so the scanner can tell where the homes separate.

### New town homes and mixed households (0.44.46 — opt-in)

The new housing registry is independent of the previous building-marker registry. **Main NPC housing only** is being migrated first; workplaces, market stalls and service jobs will be revisited separately. Existing world buildings, residents, work markers and special NPC anchors are preserved.

1. Hold the **Town Register**, right-click an ordinary **bell** to found a town. Another normal click shows its house/bed/resident counts.
2. Build a **walled, walkable house with real beds** and right-click its **front door** with the Town Register. The server performs a one-off bounded interior scan, on already-loaded chunks only. Door registration does not create NPCs automatically.
3. Optionally select an existing ordinary **Main NPC** using the Town Register, then **sneak-right-click the registered door** to give them a vacant bed. Every bed can belong to at most one resident.
4. **Sneak-right-click the town bell** to enable/disable automatic move-ins. This is OFF by default. With a nearby player and safe loaded interior, at most one Main NPC moves into the dimension each minute. Households vary between singles, couples, families and roommates (limited by beds); new NPCs receive saved identities and social relationships.
5. Clicking the front door again **refreshes** its rooms/beds without deleting residents. A missing bed leaves its resident temporarily unassigned until a vacant bed is found. Unloaded residents remain in persistent home records and are never respawned as replacements merely because a chunk unloaded.

**Extra-large house scanning (0.44.48):** The one-click house scanner extends to **128 blocks horizontally and 48 blocks vertically** from the clicked entrance, with a hard limit of **25,000 traversed indoor cells**. It now distinguishes roofed, walkable interior rooms from uncovered streets and gardens, so a second open doorway no longer automatically makes the scan flood the whole village. The scan still never forces chunk loading; if distant rooms are unloaded, an otherwise valid house can register provisionally with its detected beds and reports that the scan is partial. Rescanning a partial home merges detected beds rather than erasing existing bed assignments. The town-to-house registration radius is also **256 blocks** from the bell. Re-click registered house doors to refresh them.

**Larger house scanning (0.44.47):** The scan now reaches up to **48 blocks horizontally and 16 blocks vertically from the registered front door**, with an independent **12,000 walkable-cell work limit**. Larger houses, connected staircases and multi-storey builds fit without allowing unlimited outdoor scanning. If the flood reaches open ground, an unloaded chunk or the safety limit, the house must still be enclosed; the scan never loads chunks. Re-register an existing home by clicking its front door again to update detected beds.

**Claimed-door glow (0.44.47):** Holding the **Town Register in either hand** reveals a soft mint-green particle frame around already registered house doors within **28 blocks**. Only the player holding the Register sees the effect, and only around registered doors in loaded chunks. It stops automatically when you put the Register away. This is visual feedback, not a solid marker or permanent world effect.

Home scans do not force chunks to load, and refuse open ground or oversized interiors. Close gaps and have a connected, reachable bedroom if the scan fails; the new scanner has a 128-block horizontal and 48-block vertical envelope around the door. Bell and door interactions with the Town Register take precedence over vanilla bell ringing/door opening; use an empty hand for ordinary block use.

**Old housing markers (legacy compatibility only):** the previous setup used **four invisible marker blocks**, with small interaction/selection outlines but no normal collision:

| Marker | Purpose |
| --- | --- |
| **Building Marker** | Scans and records a bounded interior building area using the surrounding solid walls. |
| **Room Marker** | Extends the same building record into another room or section. Multiple room markers are supported. |
| **Bed Marker** | Links the nearest usable bed within 8 blocks to that building. Residents can use available beds. |
| **Work Marker** | Records the exact working block and facing direction for an assigned NPC. |

For old marker-based buildings, a convenient setup order is **Building → optional Room → Bed → Work**. Room/Bed/Work markers placed before the Building Marker can retry linking when interacted with later. Removing a main Building Marker removes its building record.

Use the **Town Register** to select an NPC, then right-click a Building Marker to link its home and a Work Marker to assign its job. Main NPCs can travel to work during the day and return to their linked home/bed at night; special NPCs can also update their stored home/work anchors. Sneak-right-click marker removal and old-world data migration remain supported.

The previous **Building Planner and legacy marker items** remain registered for save compatibility, but are not the recommended setup method.

### Town services

- **Guards:** guard posts, guard assignment, patrols, bell/alert responses, combat assistance and threat defence.
- **Pope:** church routines, waiting positions and the scripted first-arrival greeting.
- **Banker / currency:** persistent player balances, copper/silver/gold/platinum/dragon coins and banking interactions.
- **Shopkeeper:** basic buying/selling using the coin economy.
- **Courier / mail:** letter paper, addressed/sealed/opened letters, drop boxes, letter boxes, courier satchels and delivery routes.
- **Guild Receptionist:** registration and turn-in of CyberQuest's physical Guild Board contracts when CyberQuest is installed.

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

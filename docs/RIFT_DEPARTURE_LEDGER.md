# NPC Rift departures and developer ledger — 0.45.11

Install matching CyberNpc JARs on client and server (network protocol 11).
The existing player credits, confirmation, intro, friendship quests and housing
remain unchanged. This update indexes completed player departures and permits
authorised Main Seven crossings.

## Developer workflow

Give yourself the operator-only read-only ledger:

    /give @s cybernpc:rift_departure_ledger

Right-click it in the air, or run /cyber npc rift ledger. The two tabs show
the seven named character statuses and completed player departures. A character
marked Not departed is not necessarily currently spawned. Dates use UTC.
The player tab shows the latest 2048 entries if there are more; all records
remain saved. Older offline players are indexed when they next connect,
not by scanning playerdata files.

Place an Island Rift (admin item) and use a backed-up test world. Select
one nearby named NPC with a single-entity selector, e.g.:

    /cyber npc rift arm @e[type=cybernpc:cyber_npc,name=Kai,sort=nearest,limit=1,distance=..10]

Authorisation expires after 60 seconds of server game time, or when the
server stops. Natural touching without authorisation does nothing.
NPCs must be awake, living, free of mounts/leashes, not zombifying or
assigned to a special service. Arm accepts only the Main Seven stable IDs.
Physically move the NPC into the tear, or use:

    /cyber npc rift cross @e[type=cybernpc:cyber_npc,name=Kai,sort=nearest,limit=1,distance=..16] <x> <y> <z>

Use the coordinates of either half of an intact Rift within 16 blocks
of the NPC. This explicit test helper places the NPC inside the visible
tear and runs the same validated contact handler; it is not an animated
escort or a teleport to another series. If it fails, the NPC remains.
Use disarm with the same selector to cancel before crossing.

The server records the stable ID, UUID, name, date, dimension, game time
and full entity NBT (including appearance/equipment) in overworld SavedData
cybernpc_rift_departures. It then releases claims and discards the entity,
without killing or dropping loot. Duplicate loaded characters block departure;
already-departed IDs cannot rejoin from old chunks or spawn eggs.
Private snapshots stay on the server. Player relationship data remains
owned by the respective player/quest systems; no cross-series export exists.

To recover the saved NPC at a clear spot, use:

    /cyber npc rift restore kai <x> <y> <z>

Other names and stable IDs also work. Choose a loaded position with clear
space and solid ground outside the Rift. This restores the saved NPC with
its original UUID and clears the record only on a successful spawn.
Back up saves: entity and SavedData persistence are not one atomic
transaction, so hard crashes during recovery still need testing.
The original player reset/recover commands remain; they clear ledger
records when the original departure flag is reset, including offline
recovery on login.

## Manual tests

- Ordinary NPCs, wild NPCs and unauthorised Main Seven must not leave.
- Arm, disarm, expire, then arm/cross each named character in a test world.
- Check both ledger tabs, restart, attempt duplicate spawn and restore.
- Verify appearance/equipment/UUID on recovery and no loot on departure.
- Finish a player finale and verify the completed ledger entry; abort/reset
  or recover and verify the index follows the original departure flag.
- Non-operators cannot open the ledger or use departure commands.
- Test old player departures on login, both Rift halves and small GUI scales.
- No auto departure after a personal quest or changes to intro/outro playback.

Source implementation only; full Forge compilation and gameplay tests required.

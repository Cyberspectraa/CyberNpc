# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.9.1

### Pen and lead safety fixes

v0.9.1 rebuilds livestock transfer around a safer state machine.

- NPCs no longer attach a lead to an animal from far away.
- The NPC must first walk close to a reachable animal before attaching the real Minecraft leash.
- Vanilla 1.20.1 snaps PathfinderMob leashes beyond 10 blocks and drops a lead item. CyberNpc now keeps transfers below that distance and abandons the transfer safely if the animal falls too far behind.
- If a leash breaks unexpectedly, that animal is temporarily blacklisted instead of being instantly re-leashed over and over. This stops the lead-item duplication/server-spam loop.
- Only one livestock animal is moved per trip, avoiding two animals stretching in different directions.

### Full pen claims

Pen ownership is now discovered independently from hunger.

- A nearby pen is claimed as soon as the NPC discovers it rather than waiting until it becomes hungry.
- The persistent world claim covers the whole detected enclosure, not just one anchor coordinate.
- Another NPC cannot claim an overlapping enclosure that already belongs to someone else.
- Pen cell ownership survives saves and reloads.

### Better pen entry and exit

The NPC now calculates a holding position using the walkable pen cell farthest from all detected gates.

- All gates stay shut while the NPC approaches with livestock.
- Only the selected entry gate opens when the NPC reaches it.
- The livestock is taken all the way to the safe holding point at the back of the pen before its lead is removed.
- The delivered animal is temporarily held near that safe point while the NPC exits.
- The NPC deliberately walks to the inside face of the chosen exit gate, then through to the outside.
- Other gates stay closed while it leaves.
- Once the NPC is outside, every gate is closed and the temporary livestock restriction is removed.

### Other current systems

CyberNpc also includes persistent beds/sleep, hunger and starvation, ground-food pickup, furnace/smoker/campfire cooking, persistent cooking claims, sword and ranged combat, hostile-mob reactions, hunting target switching, player-style animations, Developer Glasses debugging, and natural Wild NPC spawning.

Building and mining remain intentionally excluded.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.9.0

### Hostile encounters
Wild NPCs now participate in hostile-mob encounters. Hostile Enemy mobs can acquire CyberNpc entities as targets. Endermen have special gaze handling: direct NPC eye contact with line of sight can provoke them. A Wild NPC evaluates the threat and either uses its normal combat system or retreats. Dangerous encounters such as Creepers favor retreat. Retreating NPCs search for reachable covered positions farther from the threat and can use their claimed bed as a fallback safe place.

### Developer Glasses
Developer Glasses are a head-slot item in Tools & Utilities. While wearing them, look at a CyberNpc to see live debug information: name/type, health, hunger, role, aggression, current activity, current target, server path next node and destination, claimed bed/cooking station/pen, and stored survival items.

### Cooking station claims
Cooking claims persist in world SavedData. Furnaces and smokers allow one NPC claimant. Campfires allow up to four. NPCs remember their station across saves and long travel. Distant returns use local pathfinding legs until the real station is reachable again.

### Food and hunting
Hungry NPCs can collect useful food already on the ground. Raw meat goes through the cooking system; suitable ready-to-eat food can be consumed directly. During a hunt, an NPC can re-evaluate nearby prey of the same species and switch when another target is much more practical. Managed pen animals remain protected from normal hunting.

### Corrals
Existing corrals can be claimed by one NPC only. Livestock being moved into the pen are actually leashed to the NPC, giving the normal visible lead connection. The pen detector records multiple gates, chooses an entry route, releases the animals after delivery, then deliberately routes the NPC back outside and closes the gates.

### Existing systems
The mod retains persistent beds/sleep, hunger and starvation, real furnace/smoker/campfire cooking, sword plus bow/crossbow combat, adaptive ranged aiming, stalking/sneaking, player-style animations, swimming, door use, climbing support, group aggression, and natural Wild NPC spawning.

Building and mining remain intentionally excluded.

## Development target
Minecraft 1.20.1, Forge 47.4.10, Java 17.

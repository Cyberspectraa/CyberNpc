# CyberNpc

CyberNpc is a custom NPC framework for **Minecraft 1.20.1 Forge**, built for the CyberSpectra modpack.

## Current release: 0.8.0

### Player-style animation system

CyberNpc now drives its humanoid model through Minecraft's player animation states instead of relying on generic mob defaults.

Current animation coverage includes:

- Walking and normal limb movement.
- Faster movement/sprinting animation driven by actual movement speed.
- Crouching/sneaking with the player-style render offset.
- Swimming arm/leg animation and player-style swimming body rotation.
- Jumping/falling movement through the normal living-entity/player model pipeline.
- Riding pose when the NPC is a passenger.
- Main-hand melee attack animation.
- Normal held-item pose.
- Shield/blocking pose when a used item reports the BLOCK animation.
- Bow draw pose.
- Crossbow charge and charged-hold poses.
- Spear/trident-use pose.
- Spyglass pose.
- Goat-horn pose.
- Brush pose.
- Food/item-use state while eating.
- Normal hurt/death rendering inherited from Minecraft's living-entity renderer.
- Real sleeping body rotation aligned to the bed.

The renderer now shows any item the NPC is actively holding, including combat weapons, food, and animal-lure items.

### Reliable sword swing

Wild NPC melee attacks use a dedicated synchronized swing timer. The client no longer depends only on the generic mob swing event to decide whether the PlayerModel should animate an attack.

The synchronized attack progress is fed directly into Minecraft's normal humanoid attack animation on the same attack that deals damage.

### Sleeping

- Wild NPCs keep their persistent claimed bed.
- They use Minecraft's real sleeping pose and bed orientation.
- Look-at-player and random-look goals are disabled while sleeping.
- The custom player model locks the sleeping head straight with the body, so the head no longer tracks entities or visibly moves around in bed.
- NPCs still wake for daytime, combat, damage, a removed bed, or urgent hunger.

### Corrals and hunting

Managed corral animals are now protected from the normal hunting target search.

- A Wild NPC no longer sees animals inside its managed pen as ordinary prey.
- The corral can still be stocked and bred.
- Hungry NPCs search for suitable adult prey **outside** the managed pen.
- Babies remain protected.
- The old behavior that immediately selected a surplus penned adult for hunting has been removed.

This avoids the NPC maintaining a breeding pen and then treating the same herd as its first/default hunting target.

### Existing survival AI

Wild NPCs retain hunger, starvation, adaptive bow/crossbow combat, stalking, food pickup, real furnace/smoker/campfire cooking, eating, door use, swimming, climbing support, corrals, bed claiming, and nearby-Wild-NPC combat assistance.

Building and mining remain intentionally excluded.

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

# Changelog

## 0.7.0

- Added the missing visible sword-swing animation to Wild NPC melee attacks.
- Added persistent Wild NPC bed claiming.
- Wild NPCs search for an unclaimed nearby bed at night and remember that bed across saves.
- Nearby loaded Wild NPCs avoid deliberately claiming the same bed.
- NPCs path to their bed and use Minecraft's real sleeping state and bed orientation.
- Beds are marked occupied only while the NPC is actually sleeping.
- NPCs wake at daytime, when the claimed bed disappears, when combat starts, or when they take damage.
- Hungry NPCs continue their survival behavior instead of going to sleep when hunger is already at the hunting threshold.

## 0.6.0

- Replaced the simulated cooking timer with real furnace/smoker/campfire interaction.
- Added sustainable livestock/corral management using existing fenced pens.
- NPCs can lead breeding pairs into corrals, breed them, protect babies, and hunt surplus adults.

## 0.5.0

- Reworked Wild NPC ranged combat into a visible bow/crossbow state machine.
- Added adaptive aiming, stalking, and improved player-like movement.

## 0.4.0

- Added two-weapon Wild NPC combat.
- Added hunger, hunting, food pickup, cooking, eating, and starvation.

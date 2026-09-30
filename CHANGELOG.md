# Changelog

## 0.8.0

- Protected animals inside a Wild NPC's managed corral from the normal hunting target search.
- Removed automatic hunting of surplus penned adults so outside prey is selected instead.
- Added a dedicated synchronized melee-swing animation timer.
- Wild NPC sword hits now feed explicit attack progress into Minecraft's normal humanoid/player attack animation.
- Disabled look-at-player and random-look goals while an NPC is sleeping.
- Added a CyberNpc-specific PlayerModel that locks the head in place while sleeping.
- Reworked the renderer to mirror vanilla player arm-pose selection for held and used items.
- Added player-style swimming body rotation and crouch render offset.
- Added support for vanilla player arm poses including blocking, bow, spear, crossbow charge/hold, spyglass, horn, brush, and ordinary item use.
- Replaced the combat-only held-item render layer with a general held-item layer so food and lure items are visible while being used.

## 0.7.0

- Added persistent Wild NPC bed claiming and real sleeping.
- Added the first melee swing call.
- Sleeping NPCs wake for daytime, combat, damage, removed beds, or urgent hunger.

## 0.6.0

- Replaced simulated cooking with real furnace/smoker/campfire interaction.
- Added sustainable livestock/corral management using existing fenced pens.

## 0.5.0

- Reworked Wild NPC ranged combat into a visible bow/crossbow state machine.
- Added adaptive aiming, stalking, and improved player-like movement.

## 0.4.0

- Added two-weapon Wild NPC combat.
- Added hunger, hunting, food pickup, cooking, eating, and starvation.

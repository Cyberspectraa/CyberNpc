# Changelog

## 0.4.0

- Removed the experimental YDM/body-mounted passive weapon rendering.
- Wild NPCs now own exactly two weapons: one sword and one bow or crossbow.
- Added automatic melee/ranged weapon switching during player combat and hunting.
- Added persistent Wild NPC hunger on a 0-20 scale and starvation damage at zero hunger.
- Added a visible text hunger bar when interacting with Wild NPCs.
- Added `/cybernpc hunger <target> <0-20>` for testing.
- Added health-based prey assessment and food-hunting behavior.
- Added configurable prey, raw-food, sword, and ranged-weapon data tags.
- Wild NPCs collect raw food dropped after a successful hunt.
- Wild NPCs search for nearby campfires, furnaces, or smokers, cook their collected food, and eat it.
- Added migration from the old single stored weapon when it was a sword.

## 0.3.4

- Fixed passive weapon rotations shown in the v0.3.3 test screenshots.
- Removed the depth-axis rotations that turned swords vertical and axes edge-on through the NPC body.
- Returned passive rendering to the flat FIXED item presentation while compensating its scale to retain approximately the previous visible weapon size.
- Kept the existing body/hip/leg attachment positions.
- Each attachment now only applies an in-plane diagonal rotation so the weapon lies flat against the character.

## 0.3.3

- Corrected passive Wild NPC weapon orientation using the third-person hand item transform instead of the fixed-item transform.
- Replaced the improvised passive attachment transforms with YDM's first six default attachment layouts.
- Preserved YDM's per-slot scale difference for the first body/back slot.
- Kept the existing synced stored-weapon system and combat draw/stow behavior.

## 0.3.2

- Restored visible passive Wild NPC weapons without actually equipping them.
- Synced the internally stored weapon to clients so renderers can display it while passive.
- Added a persistent stow-position value per Wild NPC.
- Added several player-style body, back, hip, and leg attachment positions based on YDM's default attachment layout.
- Combat still moves the weapon into the NPC's hand and removes the passive stored render.
- Disengaging stores the weapon again and restores its body attachment position.

## 0.3.1

- Removed CyberNpc's broken YDM-style passive back/body weapon render layer.
- Wild NPC weapons are stored internally while the NPC is passive.
- Weapons are equipped in the main hand only while the NPC has an active living target.
- Weapons are automatically stored again when the NPC disengages.
- Added automatic migration for v0.3.0 Wild NPCs that already have passive weapons equipped.
- Added a reusable weapon draw/stow hook for future food-hunting behavior without implementing hunting yet.

## 0.3.0

- Added persistent Wild NPC aggression levels.
- Added damage-based provocation: higher-aggression Wild NPCs become hostile after less player damage.
- Added nearby Wild NPC assistance within a 20-block radius without recursive alert chains.
- Added combat disengagement when the player remains more than 40 blocks away for 5 seconds.
- Added melee combat and weapon use for Wild NPCs.
- Added the data-driven `cybernpc:wild_npc_weapons` item tag for Wild NPC weapon selection.
- Added optional YDM's Weapon Master detection using its `weaponmaster_ydm` mod ID.
- Added a YDM-style stowed weapon layer for passive CyberNpc entities when YDM's Weapon Master is installed.
- Wild NPC weapons move from the body display to the hand during combat.

## 0.2.0

- Added persistent Main, Quest, and Wild NPC types while keeping one shared CyberNpc entity.
- Replaced the generic spawn egg with Main, Quest, and Wild preset spawn eggs.
- Added anvil-name support for deliberately naming Main and Quest NPCs through their spawn eggs.
- Added generated names for unnamed Wild NPCs.
- Added low-weight natural Wild NPC spawning in Overworld biomes.
- Main and Quest NPCs remain manually spawned only.
- Command-created NPCs are treated as Main NPCs.

## 0.1.1

- Replaced the generated CyberNpc texture with Minecraft's built-in Steve skin.
- Removed the bundled custom NPC texture.
- Documented that new gameplay features are only added when explicitly requested.

## 0.1.0

- Initial Forge 1.20.1 CyberNpc foundation.
- Added persistent custom NPC entity and renderer.
- Added spawn egg and custom skin.
- Added lightweight idle AI with per-NPC wandering control.
- Added persistent role data and right-click identity display.
- Added `/cybernpc` admin commands for spawn, role, wandering, and removal.
- Added automated Java 17 / Gradle 8.8 build validation.

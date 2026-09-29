# Changelog

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

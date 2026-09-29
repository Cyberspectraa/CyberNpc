# Changelog

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

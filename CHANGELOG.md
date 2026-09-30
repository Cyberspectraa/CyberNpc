# Changelog

## 0.10.0

- Reworked close-range hostile combat so Wild NPCs maintain sword spacing instead of standing inside mobs and trading hits.
- Reduced sword attack cooldown and added post-hit backoff/strafe recovery.
- Added combat assistance requests for nearby Wild NPCs when a fight becomes unsafe.
- Up to three healthy nearby Wild NPCs can join the same hostile fight.
- Critically wounded NPCs now retreat instead of remaining in combat.
- Removed crouch-running while fleeing.
- Added stalled-sprint detection to stop run-in-place animation and sprint particles when the NPC is actually stationary.
- Added carried ready-food storage.
- Added nearby chest storage for surplus raw and edible food.
- Hungry NPCs can retrieve stored food from nearby chests before hunting.
- Ready food is consumed directly; raw retrieved food returns to the cooking workflow.
- Added sustainable pen harvest: after a baby exists, one adult may be harvested, with a long cooldown preventing repeated adult kills.
- Added pen-transfer stall detection.
- Added gate realignment behavior: back away from a stuck gate, straighten the livestock, then retry entering.
- Added a hard dead-entity AI guard so released claims cannot be immediately reclaimed after death.
- Death now interrupts active pen/sleep tasks before releasing persistent claims.

## 0.9.1

- Fixed lead duplication from repeated leash snapping.
- Rebuilt pen claiming around full enclosure ownership.
- Added close-range leash attachment, safe transfer limits, deep holding points, and improved gate exit routing.

## 0.9.0

- Added hostile mob targeting and fight-or-retreat behavior.
- Added Developer Glasses.
- Added persistent cooking and pen claims.
- Added ground-food collection.
- Added hunt target switching.
- Added real leash links and multi-gate corral handling.

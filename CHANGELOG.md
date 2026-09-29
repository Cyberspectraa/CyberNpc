# Changelog

## 0.5.0

- Reworked Wild NPC ranged combat into a visible weapon-use state machine.
- Bow users now draw before firing instead of instantly spawning arrows.
- Crossbow users now visibly charge, hold, and fire their crossbow.
- Added synchronized bow/crossbow arm poses to the humanoid renderer.
- Added adaptive projectile aiming with target-motion prediction and distance-based drop compensation.
- Added distance-sensitive bow draw power and ranged inaccuracy.
- Improved combat navigation around line of sight and weapon range.
- Wild NPCs sprint while closing into melee.
- Hunting NPCs now sneak/crouch while stalking prey.
- Enabled floating/swimming and wooden-door navigation.
- Added climbable-block upward movement support.
- Kept building and mining explicitly excluded.

## 0.4.0

- Removed the experimental YDM/body-mounted passive weapon rendering.
- Wild NPCs own exactly two weapons: one sword and one bow or crossbow.
- Added melee/ranged switching, hunger, hunting, food pickup, cooking, eating, and starvation.

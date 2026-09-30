# CyberNpc

CyberNpc is a custom NPC framework for Minecraft 1.20.1 Forge, built for the CyberSpectra modpack.

## Current release: 0.13.0

### Wild NPC classes

Wild NPCs now roll a persistent combat class. Classless is deliberately dominant so special classes stay uncommon instead of taking over normal spawning.

Current spawn weights:

- **Classless — 78%**
- **Archer — 8%**
- **Knight — 6%**
- **Rogue — 5%**
- **Mage — 3%**

Class and personality are saved in NBT and synchronized to clients. Existing v0.12.0 Wild NPCs receive a class/personality once when they are first loaded under v0.13.0.

#### Classless

The normal Wild NPC.

- Keeps the existing mixed sword and bow/crossbow combat.
- Standard health, movement speed, and armor.
- Remains by far the most common spawn.

#### Archer

A ranged-control class.

- Starts with a ranged weapon plus sword backup.
- Tries to maintain more distance than Classless NPCs.
- Kites away when a target pressures it.
- Uses its sword only when an enemy gets very close.
- Fires bow/crossbow attacks slightly faster than Classless NPCs.
- Slight movement-speed increase.

#### Knight

A durable melee class.

- Primarily commits to sword combat.
- 26 maximum health.
- 5 base armor.
- Slightly slower movement.
- Holds tighter melee spacing and does not rely on ranged combat.

#### Rogue

A fast melee/flanking class.

- Primarily commits to sword combat.
- Faster movement speed.
- Shorter melee attack cooldown.
- Wider side movement and more aggressive repositioning.
- Lower maximum health than a Knight/Classless NPC.

#### Mage

A rare distance-control class.

- 22 maximum health and light armor.
- Uses a built-in magic attack with vanilla spell particles/sounds.
- Tries to remain at medium range and backs away when pressured.
- Personality changes its casting cadence and damage slightly.
- Carries a sword as an emergency backup, but its combat brain prefers magic.

Mage is intentionally self-contained for this release. **Iron's Spells 'n Spellbooks is not a hard dependency**, even if that mod is installed in the test pack. A future update can optionally teach Mage NPCs to use real Iron's Spells spellbooks without breaking CyberNpc on packs that do not include the mod.

Better Combat is also not required by CyberNpc at this stage.

### Personalities

Every Wild NPC also rolls one persistent personality independently from its class:

- **Balanced — 45%**
- **Brave — 20%**
- **Cautious — 15%**
- **Aggressive — 12%**
- **Tactical — 8%**

Personality changes combat decision-making rather than replacing the class:

- **Balanced:** standard survival/combat thresholds.
- **Brave:** stays in fights longer and is less likely to retreat early.
- **Cautious:** creates more space, retreats earlier, and asks for help sooner.
- **Aggressive:** accepts riskier fights, attacks faster where applicable, and keeps less distance.
- **Tactical:** favors spacing/flanking and requests assistance earlier than Balanced.

This means two NPCs with the same class can still fight differently.

### Developer Glasses tabbed HUD

Developer Glasses now use a cleaner compact HUD rather than one large block of debug text.

Default keybind: **V — Cycle Developer Glasses Tab**  
The keybind is configurable in Minecraft controls.

Tabs:

#### Overview
- NPC name.
- Class.
- Personality.
- Health.
- Hunger.
- Current activity.
- Zombification state.

#### Combat
- Class combat style.
- Personality.
- Aggression.
- Current combat/avoidance target.
- Current combat state.
- Health.

#### Survival
- Hunger.
- Real 18-slot inventory summary.
- Bed/cooking-station/pen claims.
- Current survival task.

#### Debug
- Raw activity.
- Target.
- Current pathfinding next node/destination.
- Claims.
- Inventory summary.
- Internal class/personality IDs.

### Existing systems

v0.13.0 keeps the v0.12.0 proper 18-slot inventory, hunger/starvation, real cooking, chest food storage, beds, full pen claims and livestock handling, hostile-mob reactions, group combat, emergency healing, Zombification/Zombie NPC conversion, player-style animations, and Developer Glasses inspection.

No farming or additional social system has been added in this release.

Building and mining remain intentionally excluded.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

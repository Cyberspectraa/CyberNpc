# Changelog

## 0.18.0

- Fixed repeated Archer/Classless bow shots missing the bow-draw pose by rendering CyberNpc's synced aiming state directly.
- Slightly reduced CyberNpc height from 1.80 to 1.75 blocks and scaled the humanoid render to match.
- Retuned Wild NPC movement toward player-like walking pace and changed idle wandering from 60% to full navigation speed.
- Added strong route avoidance for lava, fire, campfires and other damaging terrain, plus an emergency escape if an NPC is pushed or spawned onto a hazard.
- Added proactive Warden awareness: nearby uncommitted Wardens cause quiet crouched retreat; a Warden that targets or gets dangerously close causes immediate full-speed escape.
- Refined Mage spell ranges from Iron's actual spell implementations, including true melee spacing for Flaming Strike/Divine Smite and self-AoE spacing for Frostwave/Shockwave.
- Long Mage casts now obey Iron's own shouldAIStopCasting rule where provided, improving spell-specific range/target behaviour.
- Added rarity-aware death loot: common items drop more often than uncommon/rare/epic items, enchanted items count as one rarity tier higher, and stronger gear tiers improve drop odds.
- Stackable inventory loot now drops a gear-tier-scaled fraction instead of always dropping the whole carried stack.
- Mage spellbooks remain guaranteed death loot and still drop empty.


## 0.17.0

- Reduced gap jumping to a conservative 1-block walking gap and up to 3 blocks while sprinting; larger/unsafe gaps remain pathfinding detours.
- Reduced hostile threat reevaluation interval from 10 ticks to 4 ticks so NPCs react much faster when combat conditions suddenly change.
- Wardens are treated as exceptional threats in confidence calculations and now overwhelm ordinary backup confidence much more strongly.
- Starting a flee now immediately cancels an active Mage cast and switches to sprint escape behaviour.
- Wardens are excluded from CyberNpc's generic hostile-target injection so their normal vanilla vibration/anger/smell behaviour remains authoritative.
- Added visible-sculk awareness during normal movement: Wild NPCs crouch only when a nearby sculk sensor/calibrated sensor is actually visible rather than through walls.
- CyberNpc crouching explicitly counts as careful stepping, allowing vanilla vibration mechanics to suppress appropriate movement vibrations; normal/noisy movement still uses vanilla entity game events.
- Wild NPC ranged attacks now detect friendly NPCs in the firing corridor and reposition instead of firing through them.
- Friendly Wild NPC damage is ignored as a final safety net for arrows and Iron's spell/AoE crossfire.
- Mage ranged/control spells re-check the friendly-fire lane immediately before casting.
- Mage casting visuals now reuse the existing rendered offhand spellbook instead of replacing it every tick, reducing unnecessary modded-model render churn.
- Hunting prey now also passes through the combat-confidence system rather than using health alone.

## 0.16.0

- Added combat confidence scoring for Wild NPCs using mob aggression class, current health, estimated damage, NPC health/damage and nearby backup.
- Passive, neutral and aggressive mobs contribute different threat weight; actively targeting mobs become more dangerous.
- NPCs now ask nearby Wild NPCs for help when confidence is uncertain and only flee when the odds are genuinely poor or health is critically low.
- Fleeing NPCs can re-evaluate the fight and re-engage if backup arrives or the threat becomes manageable.
- Added player-scale gap jumping: up to 2-block walking gaps and up to 4-block sprint gaps, with clear-body/landing checks; unsafe or oversized gaps remain normal pathfinding detours.
- Mage spell cooldowns are now tracked per spell instead of one global spell cooldown.
- A Mage can rotate to another ready spell while one cools down; if no useful spell is ready it sprints/kites away until a spell becomes available.
- Every newly generated Mage spellbook now requires at least 2 usable spells and guarantees at least 1 direct attack spell.
- Existing invalid Mage books with fewer than 2 useful spells or no attack spell are automatically rerolled by the existing repair path.
- Developer Glasses now show combat confidence and add a dedicated Spells tab with the Mage's actual spells, levels, tactical roles and live cooldowns.

## 0.15.0

- Added tactical spell awareness to Wild Mage NPCs.
- Mage AI now profiles its rolled Iron's spells as close-range, ranged, control, healing or defensive abilities.
- Mages choose only from spells actually present in their own randomized spellbook.
- Spell choice is now scored by target distance, Mage health and spell role instead of random selection.
- Close-range spellbooks make the Mage close distance; long-range spellbooks make the Mage create space.
- Healing is prioritized when the Mage is injured, while defensive magic becomes more attractive under pressure.
- Long casts are cancelled if the target escapes far outside that spell's tactical range.
- Unknown future Iron's spells use a conservative mid-range fallback rather than breaking Mage AI.

## 0.14.3

- Fixed Mage spellbooks appearing empty and leaving Mages unable to cast.
- Mage spellbooks are now populated through Iron's real 1.20.1 ISpellContainer API instead of relying on CyberNpc-written legacy NBT.
- Mage AI reads the same real spell container that Iron's tooltips and spellbook systems read.
- Added automatic reroll/repair when an existing Mage owns a book with no usable combat spells.
- Kept compatibility reading for spellbooks created by earlier CyberNpc 0.14.x builds.
- Mage death loot still drops the same Iron's spellbook type empty; only the death copy is emptied.

## 0.14.2

- Mage NPC spellbooks remain populated with a randomized set of usable spells while the NPC is alive.
- Mage combat continues to read the NPC's actual spellbook, so a Mage can only cast spells that were rolled into that individual book.
- Killing a Mage now drops its real Iron's spellbook item as an empty book instead of leaking the NPC's preloaded combat spells.
- Empty Mage book drops have CyberNpc-only spellbook and school markers removed so they behave like normal player-owned Iron's spellbooks.

## 0.14.1

- Fixed a server crash when an Evocation Mage cast Iron's Spectral Hammer as a non-player entity.
- Added an explicit NPC-unsafe Iron's spell guard to both spellbook generation and live casting, so existing saved 0.14.0 Mage books are protected too.
- Replaced Spectral Hammer in the Evocation Mage pool with Fang Swirl while keeping Spectral Hammer available to normal players.

## 0.14.0

- Added persistent Wild NPC gear tiers: Standard 70%, Fine 20%, Rare 8%, Elite 2%.
- Added armor loadouts to every Wild NPC class, including Classless.
- Added progressively stronger weapons, armor, enchantments and base-stat bonuses for rarer gear tiers.
- Added class-specific Archer, Knight and Rogue equipment progression.
- Added persistent Mage magic schools: Fire, Ice, Lightning, Nature, Holy, Ender, Blood and Evocation.
- Added matching Iron's school armor for every Mage school.
- Added real persistent Iron's spellbooks to Mage NPC inventories.
- Added school-specific Iron's spellbooks where Iron's provides them, with generic Iron's books used for schools without dedicated books.
- Added actual Iron's spell-container NBT with randomly selected school spells.
- Gear tier now controls Mage spell count and starting spell level.
- Reworked Iron's compatibility to follow the real MOB casting lifecycle, including MagicData, SyncedSpellData, pre-cast, cast ticks, cast and cast completion.
- Added support for Iron's INSTANT and LONG spells.
- Continuous Iron's spells remain excluded until safe repeated-channel support is implemented.
- Added spellbook-in-offhand rendering while a Mage is casting.
- Added separate instant-cast and long-cast player-model animations based on the real Iron's cast type/timing.
- Added humanoid armor rendering to CyberNpc.
- Developer Glasses now show gear tier, Mage school and current spell.
- Curios support was not required for this implementation; the Mage's spellbook remains authoritative in the real NPC inventory.

## 0.13.1

- Made Mage conditional on Iron's Spells 'n Spellbooks.
- Removed fake CyberNpc Mage magic.
- Added optional soft Iron's spell casting integration.

## 0.13.0

- Added Wild NPC classes, personalities and tabbed Developer Glasses.

## 0.12.0

- Added the persistent 18-slot Wild NPC inventory.

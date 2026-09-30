# Changelog

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

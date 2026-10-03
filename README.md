# CyberNpc

## Current release: 0.41.0

CyberNpc v0.41.0 replaces the previous building setup with **four invisible marker blocks**.

### The four markers

**Building Marker**
- Place one inside the main part of a building.
- CyberNpc performs one bounded interior scan, using solid walls as the perimeter.
- Normal doors, trapdoors and gates connect rooms instead of stopping the scan.
- Exterior sky-visible space is rejected so an open front door does not make the scan spread through the town.
- If the safe scan budget is ever reached, CyberNpc keeps everything it already found instead of failing with a "too big" error.
- Any missed section can simply be added with a Room Marker.

**Room Marker**
- Place it inside a room/section the Building Marker did not detect.
- It runs the same interior scan.
- It automatically links to the nearby Building Marker and stores its scanned area as part of that **same Building record**.
- More than one Room Marker may extend the same building.

**Bed Marker**
- Place it near a real Minecraft bed.
- It finds the nearest bed within 8 blocks and links that bed to the nearby Building.
- Multiple Bed Markers are supported.
- Multiple linked residents choose different currently available beds at night when possible.

**Work Marker**
- Place it on the exact block where an NPC should stand while working.
- Face the direction the NPC should face before placing it.
- It links to the nearby Building.
- The Work Marker itself is the exact standing/facing position; it is not a loose area.

All four blocks render invisibly and have no collision in normal play. They keep a small selectable outline so they can still be administered when you know where they are. Sneak + right-click nearby with any marker item removes a marker.

### Linking NPCs with the Town Register

The Town Register now works with **any CyberNpc NPC**, not only special service NPCs.

1. Right-click the NPC with the **Town Register** to select them.
2. Right-click a **Building Marker** with the Town Register to link that NPC to the building as their building/home.
3. If the NPC works somewhere, right-click the wanted **Work Marker** with the Town Register.

This means an NPC can live in one linked Building but have a Work Marker in a completely different Building.

Normal Main NPCs:
- go to their linked Work Marker during the day,
- stand directly on its centre and face its stored direction,
- return to their linked Building at night,
- and use an available linked Bed Marker when one exists.

Special NPCs such as Banker, Courier, Guard and Pope continue using their persistent special-NPC records. Linking them through the Town Register also updates the relevant Home/Work anchors so their existing service behaviour continues to work.

### Marker order and fixing links

The easiest order is:

1. **Building Marker**
2. optional **Room Marker(s)**
3. **Bed Marker(s)**
4. **Work Marker(s)**
5. link NPCs with the **Town Register**

If a Room, Bed or Work marker was placed before its Building existed, right-click that invisible marker later. CyberNpc retries its automatic link instead of making you replace the whole setup.

Removing the main Building Marker removes its saved Building record. Existing secondary marker blocks can then be right-clicked after a new Building Marker is placed to link them again.

The older Building Planner and item-frame marker items remain registered only for world compatibility and are no longer shown as the normal setup system.

### Guard Posts

Guards now stand with their feet on the exact centre of a claimed Guard Post marker rather than stopping anywhere inside the old patrol-arrival radius. Shift rotation and bell responses still temporarily pull them away before they return to normal post/patrol duty.

## Earlier Wild NPC class documentation

CyberNpc v0.14.0 expanded the Wild NPC class system into persistent equipment/loadout classes.

## Gear quality

Every Wild NPC now rolls a persistent gear tier independently of class:

- **Standard — 70%**
- **Fine — 20%**
- **Rare — 8%**
- **Elite — 2%**

Higher tiers can provide stronger armor/weapons, enchantments, and small base-stat improvements. Elite NPCs are intentionally uncommon.

All Wild NPC classes now spawn with visible armor, including Classless NPCs.

### Classless
- Standard: leather armor and ordinary starting weapons.
- Fine: chainmail armor, improved iron/ranged gear.
- Rare: iron armor with stronger enchanted gear.
- Elite: diamond armor with rare high-end weaponry.
- Remains the common/default class; gear tier does not turn it into a special class.

### Archer
- Standard leather armor.
- Fine chainmail.
- Rare iron.
- Elite diamond.
- Uses bow/crossbow plus a backup sword.
- Better gear tiers improve and enchant its weapons.

### Knight
- Standard chainmail.
- Fine iron.
- Rare diamond.
- Elite netherite.
- Sword-focused melee class.
- Better gear tiers increase health/speed/attack stats in addition to equipment quality.

### Rogue
- Standard leather.
- Fine chainmail.
- Rare iron.
- Elite diamond.
- Fast melee/flanking class.
- Better tiers improve sword quality, enchantments and class stats.

## Mage schools

Mage still only exists when **Iron's Spells 'n Spellbooks** is installed.

Each Mage now rolls one persistent Iron's magic school:

- **Fire** — Pyromancer armor.
- **Ice** — Cryomancer armor.
- **Lightning** — Electromancer armor.
- **Nature** — Plagued armor.
- **Holy** — Priest armor.
- **Ender** — Shadowwalker armor.
- **Blood** — Cultist armor.
- **Evocation** — Archevoker armor.

The school is saved with the NPC and shown in Developer Glasses.

## Real Iron's spellbooks

Every Mage receives a real Iron's spellbook stored in its 18-slot NPC inventory.

Where Iron's provides a suitable school-specific book, CyberNpc uses it:

- Fire: Blaze Spell Book.
- Nature: Druidic Spell Book.
- Holy: Villager Spell Book.
- Ender: Dragonskin Spell Book.
- Blood: Necronomicon Spell Book.
- Evocation: Evoker Spell Book.

Ice and Lightning currently use an appropriate generic Iron's spellbook because the tested Iron's 1.20.1 version does not provide a dedicated school book for those schools.

The spellbook is populated with actual Iron's spell-container NBT and real spells from that Mage's school. Gear quality affects spell count and spell level:

- Standard: up to 2 school spells.
- Fine: up to 3.
- Rare: up to 3 at higher levels.
- Elite: up to 4 at the strongest initial levels.

The book is the Mage's actual persistent item. It is shown in the offhand while casting and is dropped with the rest of the NPC's inventory on death.

## Real Iron's casting lifecycle

Mage combat no longer uses CyberNpc-created magic.

CyberNpc's optional compatibility bridge now follows Iron's own mob-casting sequence:

- Reads spells from the Mage's actual spellbook.
- Uses Iron's real spell registry.
- Uses `CastSource.MOB`.
- Creates Iron's `MagicData` and server-side synced spell data for the NPC.
- Checks the spell's Iron's pre-cast conditions.
- Uses the real Iron's cast time.
- Calls Iron's server pre-cast, cast-tick, cast and cast-complete hooks.
- Supports both **INSTANT** and **LONG** spells.
- Continuous/channelled spells are intentionally excluded for this first loadout pass until their full repeated-tick behavior is supported safely.

This lets schools such as Nature use their real long-cast Iron's spells rather than being forced into fake instant projectiles.

## Spellcasting animation

CyberNpc still uses its player-shaped model, so it does not replace itself with Iron's GeckoLib mob model.

Instead, the player model now follows the real Iron's cast type/timing:

- Instant spells use a quick forward casting thrust.
- Long spells hold a sustained two-arm casting pose for the actual Iron's cast duration.
- The real spellbook is visible in the offhand during the cast.
- The spell projectile/effect, sound, damage and cast timing still come from Iron's itself.

## Armor rendering

CyberNpc now includes a humanoid armor render layer so vanilla class armor and compatible Forge custom armor can render on the NPC model.

## Curios

No extra Curios dependency was added in v0.14.0 because it is not required for NPC MOB casting: the authoritative spellbook is kept in the real NPC inventory and Iron's MOB casting can use it without a player Curios slot. This also avoids creating a duplicate copy of the book between the NPC inventory and a Curios inventory.

If a later feature specifically requires Curios-only bonuses/accessories, a dedicated soft Curios bridge can be added then.

## Developer Glasses

The tabbed Developer Glasses HUD now also shows:

- Gear tier.
- Mage school.
- Current Iron's spell being cast.
- Internal class/personality/gear/school IDs on the Debug page.

## Existing systems

v0.14.0 keeps the proper 18-slot inventory, class rarity/personality system, combat spacing and assistance, hunger/starvation, food storage, real cooking, beds, persistent claims, livestock management, Zombification/Zombie NPCs, and the tabbed Developer Glasses HUD.

Better Combat remains optional and is not required by CyberNpc.

## Development target

Minecraft 1.20.1, Forge 47.4.10, Java 17.

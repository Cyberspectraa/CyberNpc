# CyberNpc

## Current release: 0.40.1

CyberNpc v0.40.1 makes building setup deliberately simple: **Building Markers no longer scan the structure at all.**

### The whole setup rule

Put an ordinary item frame somewhere inside the build and put the matching CyberNpc marker into it.

That is enough.

There are no room scans, no corner selection, no wall detection and no maximum building-size error.

### Main markers

- **Home Marker** — private NPC home.
- **Shop Marker** — public shop.
- **Church Marker** — public church.
- **Bank Marker** — public bank.
- **Post Office Marker** — public post office.
- **Inn Marker** — public inn.

Each main marker creates a sensible fixed area around itself. Large building types get larger areas automatically; Churches intentionally cover a much larger area than Homes.

If a very large or oddly shaped build needs more coverage, place a **Public Area Marker** in the extra section. It links to the nearest main building automatically.

### Private/back areas

- **Staff Only Marker** — makes the nearby back area private to residents/workers/special NPCs.
- **Bedroom Marker** — makes the nearby area a private bedroom and detects real beds inside that small area.
- **Public Area Marker** — extends the public part of a building.

These are local override areas. You do not need to outline the room or close every archway perfectly.

### Activity markers

- **Altar Marker** — exact Church altar position.
- **Counter Marker** — exact service position for Bank/Shop/Post Office-style NPCs.

CyberNpc automatically finds the floor below the item frame for the NPC standing point.

### Simple Church example

For most Churches:

1. Put one **Church Marker** somewhere in the main Church.
2. Put one **Staff Only Marker** in the Pope's back area.
3. Put one **Bedroom Marker** near the Pope's bed.
4. Put one **Altar Marker** at the altar.

That is the whole setup.

The Church marker already covers a large area. If one distant wing is outside it, add a **Public Area Marker** there.

The Pope may use the public Church, Staff Only areas, Bedroom and Altar. Ordinary visitors may use the public areas but not Staff Only/Bedroom areas.

### Homes

A normal house normally needs only:

**Item frame + Home Marker = done.**

Nearby Main NPCs automatically claim available Homes. Beds near the Home marker are detected automatically. If the bedroom is farther away, put a Bedroom Marker there.

### Marker controls

- Right-click an existing marker to refresh it using the current simple-area rules.
- This also converts a 0.40.0 scan-based marker to the new 0.40.1 system with one click.
- Sneak-right-click with an empty hand to remove the marker and get the item back.
- Breaking the item frame unregisters the marker.
- The wooden frame becomes invisible after successful registration while the marker icon stays visible.

The old **Building Planner** remains registered only for compatibility with older worlds and is not used for normal building setup.

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

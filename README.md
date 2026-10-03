# CyberNpc

## Current release: 0.40.0

CyberNpc v0.40.0 replaces the manual Building Planner with automatic **item-frame Building Markers**.

### Building setup — the simple rule

Put an ordinary Minecraft item frame inside the room, then put the correct CyberNpc marker item into it. CyberNpc scans the enclosed walkable room once and stores the result.

The wooden frame becomes invisible after a successful setup, while the marker icon stays visible so the building remains easy to understand.

### Main building markers

Use **one main marker first**:

- **Home Marker** — private NPC home.
- **Shop Marker** — public shop.
- **Church Marker** — public church.
- **Bank Marker** — public bank.
- **Post Office Marker** — public post office.
- **Inn Marker** — public inn.

Example: place an item frame on an inside wall of the church nave, then put a **Church Marker** into it. That room becomes the main Church automatically.

Doors, trapdoors and fence gates count as room boundaries even while open.

### Extra room markers

Rooms behind doors can then be attached to the main building:

- **Public Area Marker** — another public room belonging to the building.
- **Staff Only Marker** — only residents/workers and the building's special NPCs may enter.
- **Bedroom Marker** — private bedroom; real beds inside it are detected automatically.

CyberNpc first links an extra room through a shared registered door. If that is not possible it uses the nearest main building as a fallback.

A Staff/Bedroom marker intentionally refuses to split one completely open room. Add a real wall/door/gate boundary when an area needs different access rules.

### Activity markers

These do not scan another room:

- **Altar Marker** — exact Church altar standing point.
- **Counter Marker** — exact service-counter point for shops, banks, post offices and similar workplaces.

You can mount these frames at normal wall height. CyberNpc automatically finds the valid floor position below the frame and stores the NPC standing position and facing direction.

### Example Church

A useful Church can be set up with only a few frames:

1. Put a **Church Marker** in the public nave.
2. Put a **Staff Only Marker** in a back room that only the Pope should normally use.
3. Put a **Bedroom Marker** in the Pope's bedroom. The bed is detected automatically.
4. Put an **Altar Marker** on/near the altar.

The Pope can use the public Church, staff room, altar and bedroom. Ordinary NPC visitors may use the public Church/Public Area rooms but are blocked from Staff Only and Bedroom rooms.

### Homes need almost no administration

For an ordinary house, a **Home Marker is normally enough**.

Main NPCs automatically claim nearby marked Homes. Capacity is based on detected beds; a Home with no detected bed temporarily has one resident slot. A Bedroom Marker can be used when the bed is in a separate room behind a door.

### Marker controls

- Right-click a registered marker frame to see what building it belongs to.
- Sneak-right-click a registered marker with an empty hand to unregister it and return the marker item.
- Breaking a marker frame also removes its saved marker registration.
- If a room changes substantially later, remove and replace that room's marker to scan it again.

The old **Building Planner** item is kept registered only so old 0.39.x worlds do not lose the item. It is retired and no longer appears in the CyberNpc creative tab.

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

# CyberNpc

## Current release: 0.39.0

CyberNpc v0.39.0 adds persistent building/property recognition, private-home access rules, exact Guard Post positioning and the first building-aware special role: the Pope.

### Building Planner quick setup

The planner is now designed to show you what it is doing while you hold it.

**What you see while holding it**
- The action bar always shows the selected building and the current planner tool.
- White particle outlines show every registered area belonging to the selected building.
- Green particles mark registered doors/gates.
- Hearts mark beds.
- Enchantment particles mark a Church altar.
- Crit particles mark service counters.
- A vertical flame marker shows corner 1 while you are waiting to click corner 2.

**Create a building**
1. Hold the **Building Planner**.
2. Sneak + right-click the air until the wanted building type is shown.
3. Make sure the tool says **Build / Select**. Normal right-clicking the air cycles tools.
4. Right-click one corner of the building interior.
5. Right-click the opposite corner.
6. The building is created immediately and selected. Doors/gates and real beds are detected automatically.

**Select an existing building**
- In **Build / Select**, right-click anywhere inside one of its outlined areas or on one of its registered entrances.

**Buildings that are not simple boxes**
- Switch to **Add Area**.
- Click two opposite corners around another room, floor, wing or section.
- Repeat as needed. All of those areas remain one logical building.

**Special Spot**
- Church: click the floor position where the Pope should stand at the altar.
- Shop / Bank / Post Office / Workshop: click the service-counter standing position.
- Clicking a door/gate in this tool manually adds/removes that entrance if auto-detection needs correcting.
- Clicking a bed can manually add it, although normal beds are detected automatically.

**Residents / Workers**
- Switch to **Residents** and right-click an NPC who lives in the selected building.
- Switch to **Workers** and right-click an NPC who works there.
- Right-clicking the same NPC again removes that assignment.

**Delete**
- Switch to **Delete**, then sneak + right-click inside the selected building.
- The extra sneak requirement prevents accidental deletion.

### Church example

Create one Church record covering all of the church's interior zones, mark its entrances, add an Altar and a real Bed, then spawn a Pope and set the Pope's Work/Home with the Town Register. The Pope can roam the registered Church, visit its altar each morning, acknowledge NPC visitors and sleep in the marked bed at night.

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

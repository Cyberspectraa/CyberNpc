# CyberNpc

## Current release: 0.13.1

v0.13.1 keeps the class/personality and tabbed Developer Glasses systems from v0.13.0, but changes Mage integration.

### Mage requires Iron's Spells 'n Spellbooks

Mage is now a true optional-integration class.

- Mage is only included in the Wild NPC class spawn roll when the mod id `irons_spellbooks` is loaded.
- Without Iron's installed, the Mage weight is skipped entirely and the remaining classes are re-normalized automatically.
- Any saved Mage loaded without Iron's installed is safely converted to Classless.
- CyberNpc does not hard-link to Iron's classes, so CyberNpc itself can still load without Iron's.
- Iron's is declared as an optional AFTER dependency so its registries are ready before CyberNpc uses the integration.

### Real Iron's spells only

The temporary CyberNpc-created magic attack has been removed.

Mage combat now invokes Iron's own spell registry and server-side spell lifecycle using `CastSource.MOB`.

The initial verified Mage attack pool is:
- `irons_spellbooks:firebolt`
- `irons_spellbooks:magic_missile`
- `irons_spellbooks:icicle`

CyberNpc checks that a selected Iron's spell exists, is enabled, and is an INSTANT spell before casting it. The spell itself creates the projectile/effect, sound, damage behavior, school scaling, and other Iron's mechanics.

If Iron's cannot provide a valid spell at runtime, the Mage repositions and retries later. It does not fall back to fake CyberNpc magic.

### Class rarity

When Iron's is installed:
- Classless 78%
- Archer 8%
- Knight 6%
- Rogue 5%
- Mage 3%

When Iron's is not installed, Mage is excluded from the roll.

### Developer Glasses

The Mage combat-style line now reports `Iron's Spells caster / distance control`.

The existing Overview, Combat, Survival and Debug tabs remain unchanged, with V as the default configurable tab-cycle key.

## Development target
Minecraft 1.20.1, Forge 47.4.10, Java 17.

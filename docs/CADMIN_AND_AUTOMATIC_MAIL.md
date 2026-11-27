# Approved admin commands and automatic postal routing

CyberNpc 0.45.13, CyberQuest 0.3.0-alpha.26, CyberRaces 0.3.2-alpha.77,
CyberClasses 0.2.1-alpha.14, CyberServer 0.2.8. Update all five to retire the
old registrations. The command root is lowercase /cadmin and OP level 2.
No legacy Cyber command aliases are registered by these versions.
The current full list is in the README. /cadmin help lists live usages.
/cadmin doctor checks required command paths for installed mods and known old
roots. It does not run destructive commands or verify gameplay.

## Postal setup

- Place a normal Drop Box and a Courier spawn egg within 96 blocks of it.
- Each recipient places their own Letter Box; the most recently placed box
  becomes their active address. Unowned structure-placed boxes cannot receive
  private mail until a player places one. Existing registered addresses remain.
- Keep the route and blocks loaded and reachable. Couriers search registered
  nearby pickup boxes automatically during the day; no workplace binding is
  needed. Night rest and existing direct player delivery remain.
- Actual loaded blocks and ownership are validated before collection/delivery.
- Discord imports select a loaded, non-full Drop Box in the recipient's
  dimension if known, favouring proximity to the recipient's address.
  Otherwise the normal automatic fallback prefers the Overworld.
- Old fixed Discord bindings are ignored and removed from the next save.
  Existing letters and their pickup positions are preserved.
- If a Drop Box was pasted before this update and never registered, right-click
  it once or replace it. New placements register automatically. No world-wide
  block scan or forced chunk loading is performed.

## NPC departures

The ledger retains its read-only display on normal right-click. Sneak-right-click
an eligible Main Seven NPC with the ledger to arm or cancel crossing. Permission
level 2 is required. The NPC must touch an intact Rift while armed. The old
arm/disarm/cross/restore commands are removed. No animated escort or new
NPC recovery interface is added; snapshots remain on disk. Back up the test
world before crossing. Player recovery remains available under /cadmin.

## Test checklist

Build all updated mods locally. Check /cadmin doctor and tab completion, and
verify old roots are absent. Check commands as non-OP. Confirm J journal,
class registration, race creation and quest progress remain intact. Test
new drop boxes, existing player boxes, moving a box, unloaded/full boxes,
mail to an offline recipient, and delivery without a work assignment.
Confirm normal and Discord mail remain separate from bot credentials.
Test the ledger's sneak-use on an authorised NPC and a non-OP user.
No full Forge compile or live test is claimed by this source update.

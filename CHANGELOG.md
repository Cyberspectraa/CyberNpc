# Changelog

## 0.9.1

- Fixed Wild NPCs attaching livestock leads while still far away.
- Prevented the infinite lead-item duplication loop caused by repeatedly reattaching a leash after vanilla distance breakage.
- NPCs now approach an animal first and only attach the lead at close range.
- Added leash safety distances and transfer abort handling.
- Unexpectedly broken leads blacklist that animal temporarily rather than immediately recreating the leash.
- Changed livestock transfer to one animal per trip for more reliable movement.
- Pen claims are now discovered independently from hunger.
- Pen ownership now persists the full detected enclosure cell set instead of only an anchor position.
- Overlapping claimed pen cells cannot be claimed by another NPC.
- Added a calculated livestock holding point at the part of the enclosure farthest from detected gates.
- NPCs carry livestock to the holding point before releasing the lead.
- Delivered livestock stays near the safe point temporarily while the NPC exits.
- Reworked gate handling so only the needed gate is open.
- Reworked pen exit pathing through the inside gate tile and then the outside tile before gates close.

## 0.9.0

- Added hostile-mob targeting and fight-or-retreat behavior.
- Added Developer Glasses.
- Added persistent cooking and pen claims.
- Added ground-food collection.
- Added hunt target switching.
- Added real leash links and multi-gate corral handling.

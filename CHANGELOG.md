# Changelog

## 0.11.0

- Added low-health emergency retreat and eating.
- Real item consumption now applies vanilla food effects such as golden-apple regeneration and absorption.
- Emergency food also restores immediate health based on nutrition.
- Chest food retrieval now ranks ready food so stronger recovery items are preferred.
- Wild NPC death now guarantees dropping the full stored/carried inventory rather than relying on ordinary mob equipment-drop chances.
- Added the Zombification harmful effect with a 2-minute duration.
- Zombie-family hits have a 1% chance per successful hit to apply Zombification.
- Hostile mobs stop targeting infected NPCs.
- Nearby Wild NPCs become increasingly likely to avoid an infected NPC as its conversion timer approaches zero.
- Added a real Zombie-derived Zombie NPC entity using vanilla zombie AI, appearance, sounds, daylight behavior, and loot.
- Zombification expiry converts the infected NPC into a Zombie NPC.
- A zombie-family kill has a separate 5% chance to immediately raise the dead NPC as a Zombie NPC.
- Zombie NPC attacks can infect Wild NPCs through the same Zombification system.
- Claims are released before death conversion or effect conversion.

## 0.10.0

- Improved close-range hostile combat spacing and group assistance.
- Added sprint-stall cleanup and removed crouch-running while fleeing.
- Added food chest storage/retrieval.
- Added controlled pen harvesting and gate realignment.
- Fixed claim release after death.

## 0.9.1

- Fixed lead duplication and rebuilt whole-pen claiming.

## 0.9.0

- Added hostile mob targeting, Developer Glasses, persistent claims, ground-food pickup, target switching, and real livestock leads.

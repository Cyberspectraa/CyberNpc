# Pope — first meeting and guidance (0.44.67)

The Pope is the first person a player meets after their summoning cinematic.
He **did not summon the player** and **does not know who or what summoned them,
or why**. His duty is to keep newcomers safe and explain the town.

## First encounter

The existing intro now provides a seven-part, skippable conversation:

1. Arrival: Pope confirms he did not summon the player and doesn't know why.
2. Location: the player is on a vast island with a town for refuge and trading.
3. Wilderness: equipment, food, and a clear warning not to go far north.
4. The North: ship graveyard and **Shipbreaker** (Aquamirae).
5. Starting out: Adventurers' Guild contracts and preparing for the trip.
6. Trade: copper/silver/gold coins and no emerald economy.
7. Parting guidance: unknown beasts and dangerous magic, start near town.

This island's north location is world/story-specific; it isn't inferred from
Aquamirae's biome placement. The warning deliberately avoids spoilery boss
mechanics, unverified world structures, or invented quest objectives.

## Return visits

The Pope now has six informational conversation choices about summoning,
the north, getting started, other dangers, trade, and the church. Responses
never take or award items, accept contracts, or assume services that are not
implemented. The Pope's greeting changes after the initial meeting.

## Technical safety

- Intro continues through CyberIntroService.finish, retains Skip introduction.
- Server-side dialogue action IDs are validated against offered options.
- Other NPCs still have their speech intentionally blank from 0.44.66.
- Wild NPC reaction icons and voice noises untouched.
- No changes to conversation packet, item IDs, save data or world generation.

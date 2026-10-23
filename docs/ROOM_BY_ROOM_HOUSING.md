# Per-NPC room-by-room housing (0.44.72)

## Usage
- Get `cybernpc:home_assignment_tool` from the CyberNpc creative inventory or `/give @p cybernpc:home_assignment_tool`.
- Right-click an ordinary **Main** NPC (not a Wild NPC or scripted service NPC).
- Right-click the top of a solid **floor** tile inside the room. Only that connected floor level is scanned; a wall, closed/open door or gate stops the scan. Click the floor of each further room and floor separately, up to **10 rooms per NPC**.
- Sneak + right-click any saved room's floor removes the whole saved room. Sneak + right-click air clears the *selected NPC* from the tool; it does not erase a home.
- Rooms require a roof and at least one actual door; open hallways/outdoor leaks, oversized rooms and unloaded chunks are rejected without saving a partial scan. A home may have rooms without beds, but beds detected inside are preferred for sleeping.

## Compatibility
- Stores NPC room footprints, door coordinates and beds in the existing `cybernpc_town_homes_v1` SavedData under the **optional** new `NpcRoomHomes` list. Existing `Towns`, `Homes`, resident UUIDs, beds and door links remain unchanged.
- Explicit floor room assignments take priority in the established `assignedHome` lookup. The existing marker-based and front-door homes still work for older saves and as fallback if every explicit room is removed.
- A room-based home's nearest registered town supplies `residentTownId` for existing town-work requirements. A bell/town registration is **not required** just to assign rooms.
- Ordinary Main NPCs use the same night-time bed and home-return AI; no changes to service-role AI, wild NPCs, class/quest saves or workstation configuration.
- The item only changes housing when right-clicked. No hidden room scans during normal game ticks and no forced chunk loading.

## Test checklist
1. Select a Main NPC with the tool and save a room with one door; test an open door also remains a boundary.
2. Save a second room through the door and a room upstairs; confirm `2/10`, `3/10` and no vertical scan leaks.
3. Test double doors, beds, carpets/furniture, same-room duplicate, and 10-room capacity. Eleventh room should be refused.
4. Remove one saved room by sneaking, then clear tool selection in air; verify the other saved rooms persist through reload.
5. At night, verify assigned beds, no bed sharing with other registered households, and no forced teleporting.
6. Attempt an outdoor or overlarge scan: confirm no partial home is written.
7. Confirm existing door/bell registrations and older Building/Bed/Work markers still work.

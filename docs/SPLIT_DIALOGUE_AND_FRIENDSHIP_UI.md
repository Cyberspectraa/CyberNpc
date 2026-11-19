# Split-corner dialogue screen + real Main Seven friendship stats

CyberNpc 0.44.93 — replaces the previous full-width bottom dialogue bar.

## Layout
- Bottom-left: current speaker's name, portrait (where space permits) and
  compact parchment speech panel. Background is the actual Minecraft world,
  with no whole-screen dimming.
- Bottom-right: up to four visible reply options, wrapped across two lines
  when needed. Hovering preserves the complete option via tooltip.
  Next/previous buttons page through the exact original server-supplied
  choices. This avoids losing options at low screen resolution.
- Long speech may be scrolled backwards using the mouse wheel over the left
  panel. The current/newest text remains visible as the typewriter advances.
- Speaking swaps NPC/player portrait and name in the left speech panel.
  Both existing NPC grunts and the new player reply voice continue to work.
  Two clicks still reveal reply text and then the queued server response.
- Existing story intro, Skip/Finish, buttons, quest actions, server-side
  validation, session handling, Rift and voice asset IDs are unchanged.

## Main Seven friendship
- For named character NPCs only, the speech panel includes two **actual**
  saved relationship stats: `Trust` and `Affection` (each -10 to +10).
- Displays an accompanying **descriptive** bond label:
  Strained; New acquaintance; Familiar; Friendly; Close; Trusted.
  These labels do not change any game mechanics. In particular the existing
  secret-quest friendship requirement (Trust >= 2, Affection >= 1) is
  preserved. The values are per-player and copied from the existing
  CyberNpcTownCharacters NBT; no duplicate friendship levelling.
- For the Pope, Guild Receptionist and ordinary shopkeepers there is no
  invented relationship stat panel.
- The server attaches the values to the dialogue snapshot. After a choice
  alters trust/affection, the response brings updated values. Other players
  see only their own values.
- The dialogue packet has been extended with optional relationship values:
  network protocol is now **6** (both client and server need 0.44.93).

## Suggested in-game tests
1. Open the Pope and the seven Main NPCs with different GUI scales.
   Confirm the split panels stay inside the screen and still fit choices.
2. Click a trust-raising and affection-raising line. After the response,
   confirm the exact values update. The same response from a different
   player should not share the first player's values.
3. Open a node with 5–8 options. Page through, select an option on the
   second page and verify it advances the intended dialogue.
4. Try a long Pope explanation. Scroll the speech left panel to reread,
   then choose a reply. Confirm no permanently clipped content.
5. Confirm player and NPC voice blips, manual two-click dialogue pacing,
   intro Skip/Finish, and quest progression are unchanged.
6. Confirm screen can close safely and no dialogue UI remains after walking
   away or after resetting the test intro.

Source changes only; local compilation and Minecraft playtesting pending.

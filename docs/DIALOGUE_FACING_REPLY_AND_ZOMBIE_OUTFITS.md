# CyberNpc 0.44.94 — Dialogue polish, facing and zombie clothing

## Dialogue UI
- Keeps the split-corner screen that the series owner selected.
- The **left** side always shows the NPC (with real per-player Trust and
  Affection when applicable), even while a player reply is spoken.
- The **right** side switches from choice buttons to a short, fixed-size
  parchment reply card with typed text, the local player voice sounds, a
  waiting indicator, and a usable Continue or Close button.
- No longer replaces the NPC's portrait with the player or renders a huge,
  empty waiting panel.
- Tightens Trust/Affection spacing and gives speech more vertical room.
- Temporarily hides the NPC's floating nameplate only during the small
  client-side portrait render, then restores the entity setting.
- If there are no valid options, a **Close** control is still available.
- If the server response fails or is delayed (8+ seconds), the client
  indicates that no response arrived and can exit without being trapped.
- Server-side invalid/stale choices refresh safely without repeating quest
  rewards or inventing a new dialogue choice.
- Explicit Escape/Close sends an id-validated close packet so the NPC stops
  facing the player. Network protocol updated from 6 to 7; servers and
  clients must both update.

## NPC facing
- Only NPCs in active valid dialogues receive a small yaw/head look update
  each server tick after other idle/service AI. No expensive world scans.
- They turn smoothly to face the current player rather than flicking away
  due to random look goals.
- Stop tracking on Farewell, Escape, class UI hand-off, disconnection, invalid
  range or expired session.

## Custom zombie outfits
- Two original user-supplied transparent 64x64 PNG overlays are bundled:
  `assets/cybernpc/textures/entity/appearance/zombie/outfit_steve.png`
  and `outfit_alex.png`.
- The wide/Steve and slim/Alex variants overlay zombie skin/body UVs on top
  of the already generated face, eyes, hair and skin texture.
- Transparent head pixels protect custom zombie face/hair features; race
  rendering, vanilla ZombieModel geometry and Fresh Animations/EMF are left
  alone. All converted and naturally generated custom CyberNpc zombies use
  the same texture generation path.
- No new entity, texture randomiser, or new zombie registry IDs.

## Manual tests (not completed in this environment)
1. Use Rowan and Katie dialogues. Choose a reply: NPC remains on the left,
   chosen line and player blips appear on the right, then Continue displays
   the NPC response.
2. Open a topic with 5-8 options; page through and pick on the second page.
   Confirm progress and bond values update correctly.
3. Let reply type out, then temporarily test server rejection / stale
   choice. The window must have a working Close even if no response arrives.
4. Press Escape at different dialogue points and confirm the NPC returns
   to normal idle head movement.
5. Spawn and convert custom male/wide and female/slim zombies. Check
   shirts/trousers display and hair/eye/skin layers remain visible.
6. Confirm introduction Skip/Finish, class and quest UI handoffs,
   the Wild NPC AI, the seven personal quests, player/NPC voices and Rift
   appearance remain unchanged.

Only source and resources are committed. No Forge build or in-game test
has been completed here.

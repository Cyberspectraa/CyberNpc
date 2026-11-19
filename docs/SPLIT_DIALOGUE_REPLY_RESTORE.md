# CyberNpc 0.44.95 — Restore original split-screen player replies

This follow-up responds to in-game feedback after 0.44.94:
the repeated player reply text and new right-side parchment felt detached
from the conversation. The original 0.44.93 speaker-swap was preferred.

## Screen
- Restore the **single active-speaker left panel** from the first split UI.
- NPC speaks: NPC name, portrait, Trust/Affection and parchment dialogue.
- Player replies: switch the *same left panel* to player's name, portrait
  and voiced/typewritten reply. Do **not** repeat NPC relationship values
  under the player's portrait.
- The right panel shows response choices while NPC is speaking.
  During a player response it shrinks to a discreet 67-pixel
  Continue/Close status panel. This retains the desired speaker swap
  without restoring the older oversized empty right panel.
- Preserve scrolling for longer lines, option pagination, player/NPC voice
  events, server-owned actions, manual click-to-reveal and click-to-advance.
- Preserve no-options Close fallback, delayed-response message and explicit
  Close/ESC network packet. No quests, friendships or saves are changed.

## Facing follow-up
- Reapply NPC look direction as soon as a conversation packet is sent,
  then every active server tick.
- Strengthen the smooth yaw adjustment from ~18% to ~40% per tick.
- Stop peaceful idle navigation during conversation, without stopping
  active monster-target combat.
- Ensure fence-avoidance's early-return path also updates conversation
  facing. This addresses screenshots showing an NPC turned away during
  an open dialogue.

## Intentionally unchanged
- Zombie Steve/Alex outfit layers and custom zombie face/hair.
- Main Seven secret quests and weapons.
- CyberRaces XP guild-registration gate.
- Existing dialogue network protocol 7 and all server quest choices.
- Inert Photon island Rift.

## In-game checks (not yet performed)
1. Speak to Rowan, Katie and Pope. Confirm NPC portrait/text on left and
   options on right; on choosing a reply, player portrait/text takes over
   left panel. No NPC bond stats shown under the player.
2. Select a reply, let typewriter/voice finish and click Continue. NPC
   response must reappear on the left.
3. Test many reply choices, a long text scroll and a personal quest.
4. Temporarily cause a rejected/delayed response; right side must offer
   working Close instead of leaving the chat stuck.
5. Check Rowan facing the player and returning to normal idle behaviour
   after Farewell or Escape.
6. Check both zombie outfit variants as before.

Source committed only; local build and Minecraft tests remain pending.

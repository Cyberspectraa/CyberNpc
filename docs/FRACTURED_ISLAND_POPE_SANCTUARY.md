# The Fractured Island — Pope's Sanctuary Spell
CyberNpc 0.44.89 (development-no-actions)

## Established lore
The Fracturing damaged the boundaries between worlds. The island pulls newcomers
across those boundaries involuntarily. When someone is changed by the fracture's
curse, that happens **during the crossing**, not gradually after arrival.

The Main Seven (Kai, Katie, Elara, Rowan, Felix, Nadia, Orin), the Pope,
Guild Receptionist and Shopkeepers all arrived **after** The Fracturing,
**unchanged**. They believe they were lucky. No one is established as immune.
An unfamiliar race does not prove a person was transformed: they might have
belonged to that race before crossing.

The Pope arrived unchanged. Seeing others stranded by uncontrolled arrivals in
the dangerous wilderness, he studied magic and learned to **redirect some new
arrivals to the church**. He did not cause the fracture, cannot summon or select
travellers and cannot prevent transformation. His spell is imperfect. It
succeeded for the player and explains the church arrival cinematic.

The island's long-term fate is unresolved; the Rift remains **cosmetic only**
and is not tied to the spell yet. Rifts are to be admin-placed after major
story events, when the series owner is ready.

## Implementation scope
- Existing first-arrival Pope lines now explain the redirected landing.
- Human and nonhuman player races receive slightly different curse wording,
  read from saved CyberRaces `Race`, without assuming they were transformed.
- Pope revisit dialogue gains validated follow-up branches about his spell,
  its limits, the curse, and why he remained unchanged.
- Existing `intro_0` through `intro_7`, Skip/Finish controls, summoning
  cinematic, Pope role, and quest-linked response IDs are preserved.
- Quest actions `pope_begin`, `pope_trade`, `pope_curse`,
  `pope_north`, and `guild_info` are unchanged so CyberQuest onboarding
  can still record them in any order.
- Seven town-character friendship/dialogue scripts, Guild Receptionist
  services, Wild NPC voices, and 1x2 Photon Rift remain unchanged.
- No gameplay spell mechanics, automatic NPC summoning, worldgen, or Rift
  activation are added; this update makes existing story dialogue coherent.

## Local test
1. Build CyberNpc locally with `build-local.bat`, replace old JAR.
2. Replay the intro; confirm the Pope guides the player to the church without
   claiming to have summoned them. Skip and Finish must still work.
3. As Human and nonhuman test players, read intro_2 and the Pope's curse topic.
   Neither form alone must be labelled definitely cursed.
4. Revisit the Pope; follow the arrival/curse branches, then Back and Farewell.
5. Complete Getting Your Bearings with pope_begin and pope_trade in either order;
   Island Whispers with pope_curse and pope_north; Guild Orientation with
   guild_info. No duplicate actions or blank replies should appear.
6. Confirm the Guild Receptionist, Main Seven, Rift and Wild voices still work.

**Source committed only; build and in-game testing remain to be done locally.**

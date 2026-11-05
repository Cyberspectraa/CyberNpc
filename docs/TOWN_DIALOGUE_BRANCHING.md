# Authored Town NPC dialogue (Forge 1.20.1)

This update replaces the seven characters' generic Agree / Disagree / More / Insult topic selector
with **data-driven, branching conversations**. No new NPC entities or spawn egg IDs were added.

## IDs and content
- Kai: `mason` (legacy story ID and `mason_npc_spawn_egg` intentionally preserved).
- Katie: `town_npc_1`
- Elara: `town_npc_2`
- Rowan: `town_npc_3`
- Felix: `town_npc_4`
- Nadia: `town_npc_5`
- Orin: `town_npc_6`

Scripts live in `src/main/resources/data/cybernpc/town_dialogue/`. Each JSON file defines:
- `story_id`, `name`
- `first_greeting`, `repeat_greeting`, `trusted_greeting`, `cooldown_greeting`
- `redirect`, `subject_change`
- a `nodes` map containing a speaker `speech`, optional `remember` flag, and an array of `choices`
- each choice has `text` (the player's exact response), `to` (next scene ID), and optionally `effect`, `when`, or `action`.

Supported `effect`s: `warm` (affection), `trust`, `repair`, `rude`, `kai_other`, `kai_choso`.
A positive relationship change may occur at most once per in-game day **per character**,
and each authored decision only changes the relationship the first time it is made.
Intentional insults can damage affinity, and Kai can choose to disengage and walk away
when an interest is mocked. Disagreement is not inherently insulting.

Supported conditions (`when`): `flag:<name>`; `quest_available`; `quest_active`;
`quest_completed`. Supported quest actions: `quest_start`, `quest_finish`
(only Katie's pre-existing yarn quest). The quest itself remains in CyberQuest;
no other characters were assigned quests.

A user can edit datapacks to replace or extend conversations, then `/reload`.
Malformed profiles are skipped with a server log error. Conversations are checked
on the server against the current page before accepting a player choice.

## Testing
1. Replace **only** `CyberNpc.jar` from the previous build, keeping the
   `CyberQuest.jar` already installed for the Yarn quest.
2. Talk to all seven NPCs, note that they have different starting questions and follow-ups.
3. Ask Kai about Choso, SK8, music, games, myths, and foxes. Test polite disagreement
   separately from deliberate insults. Kai may end a conversation and walk away.
4. Speak to Katie about her crochet project, then ask whether she needs supplies.
   Confirm the Yarn quest starts, resumes when you return, and can be handed in when
   the existing CyberQuest objective is satisfied.
5. Close / reopen dialogue and change topic midway: no repeated generic replies,
   disconnected options, missing pages, or blank dialogues should appear.
6. Return to an NPC on another Minecraft day: greeting and relationship changes
   should not be farmable by repeating one answer.
7. Test two players independently and confirm each player's memory and quest state
   is separate. Test that `/reload` still loads all seven profiles without server errors.
8. Check the one-column response layout on larger screens and two-column fallback
   on small GUI heights; player replies should advance to the NPC automatically
   after a brief, readable pause (clicking still skips).

## Known limitations
- This is a *scripted* conversation graph, not open-ended natural-language generation.
- Character personalities are initial authored interpretations; they can be refined.
- Secret quests beyond Katie's already specified Yarn objective have not been authored.
- Friendships between the NPCs have not been assigned.
- Kai's Choso Doll quest is still a future design, not activated by these dialogues.
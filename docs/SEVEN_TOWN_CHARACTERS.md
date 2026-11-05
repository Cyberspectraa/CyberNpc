# Seven authored Town NPCs (test build)

Stable IDs: `mason` (Kai), `town_npc_1` (Katie), `town_npc_2` (Elara), `town_npc_3` (Rowan), `town_npc_4` (Felix), `town_npc_5` (Nadia), `town_npc_6` (Orin).

The seven existing spawn eggs retain their registry paths. Only default labels are updated. Old saved entities with the generated name are migrated when loaded; customised names are preserved. Existing skins and models are unchanged.

Server-authoritative dialogue shares one event-driven engine: personal topics, follow-up responses, opinions, once-per-topic affinity/trust, occasional walk-away reactions and brief cooldowns. Player conversation state is stored on player NBT and copied after respawn. No per-tick dialogue polling or external AI.

Katie's Yarn quest requires CyberQuest. Discuss crochet first, then ask about Yarn. Accepting is optional. Yarn's normal crafting recipe uses one String per Yarn; CyberQuest must enforce quest-active crafting and track 64 successful crafts before Katie accepts turn-in. Kai's Choso Plush quest is not activated because its script and objectives remain unspecified.

All dialogue strings are first-pass test content, not recordings of the real people. Inter-NPC friendships, origins of the five fiction-first NPCs, and undisclosed quests are not canonised.

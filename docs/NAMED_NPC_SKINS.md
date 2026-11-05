# Authored character skins (CyberNpc 0.44.85)

The nine player-made 64x64 Minecraft skins are bundled directly as PNG resources.
They replace the generated face/hair/outfit appearance only for seven authored
town characters and the Pope / Guild Receptionist service roles.

| Character | Persisted identity | Texture | Arm geometry |
| --- | --- | --- | --- |
| Kai | story ID `mason` (old custom name `Mason` still supported) | `characters/kai.png` | classic (4 px) |
| Katie | `town_npc_1` | `characters/katie.png` | classic |
| Elara | `town_npc_2` | `characters/elara.png` | slim (3 px) |
| Rowan | `town_npc_3` | `characters/rowan.png` | classic |
| Felix | `town_npc_4` | `characters/felix.png` | classic |
| Nadia | `town_npc_5` | `characters/nadia.png` | slim |
| Orin | `town_npc_6` | `characters/orin.png` | classic |
| Pope | service role `Pope` (including `Priest`, `Pontiff` aliases) | `characters/pope.png` | classic |
| Guild Receptionist | service role `Guild Receptionist` (including aliases) | `characters/guild_receptionist.png` | classic |

All paths above are under `assets/cybernpc/textures/entity/`.
Story IDs, dialogue, NPC spawn eggs, NBT, quests and voice assignment stay unchanged.
Existing saved NPCs with their story IDs or service roles receive the correct
skin on the client without migrating save files or respawning the entity.

The same shared profile determines **both** the texture and the 3-pixel/4-pixel
arm model so jackets, sleeves and the optional Skin Layers 3D meshes align.
No extra render passes or serverside texture processing are introduced.

For other Main/Wild NPCs, the original generated face/skin/hair/outfit pipeline
remains intact. The Courier and Shopkeeper appearances are unchanged.
The uploaded Pope skin intentionally replaces the old random-face Pope outfit
composition; the named Pope role now has a consistent authored appearance.

The PNGs can be overridden with a Minecraft resource pack without changing
any Java code or server data.

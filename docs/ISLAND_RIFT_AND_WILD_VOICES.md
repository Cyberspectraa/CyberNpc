# Island Rift foundation and Wild NPC voices — CyberNpc 0.44.87

Development branch: `development-no-actions`. This is a source commit
awaiting a LOCAL Forge 1.20.1 build (no GitHub Actions).

## Island Rift (phase one)

- Registered block and block item: `cybernpc:island_rift`.
- **Operator-only placement** (permission level 2+) from the item. Command:
  `/give @s cybernpc:island_rift` as an operator. An admin may also use
  `/setblock ~ ~ ~ cybernpc:island_rift`.
- Not craftable, not in a creative tab, and not naturally generated.
- Voxel pedestal with custom rift-stone / violet-core pixel textures,
  subtle particles and occasional portal ambience; emits light level 9.
- **No gameplay behaviour yet.** Right-clicking does nothing: it does not
  register discovery, save player data, play activation effects, advance quests,
  teleport players, recruit NPCs or mark departures.
- The future per-player discovery mechanism must be added only when the island
  storyline is ready. Do not add placeholder persistent state yet.
- Unbreakable in survival by default. An operator can replace the block with
  a command. This is NOT a teleport portal yet.
- No quest changes, NPC escorts, disappearing NPCs, world deletion or save
  migration in this foundation stage. Avoid implementing redundant discovery
  flags or departure data until the story system is designed.

## Wild NPC voice refresh

- Original user WAV recordings from `Generic Male.zip` and
  `Generic Female.zip` were cleaned of initial silence / long breaths,
  softly filtered and converted to 22,050 Hz mono OGG.
- Six new independent sound events/OGGs:
  `wild_voice_{male,female}_{hm,mm,uh}`.
- Wild NPCs use gender-matched voice clips for ambient, emotional reactions,
  and hurt sounds (Hm / Mm / Uh selected by context).
- Previous random ambient interval, reaction and hurt cooldowns retained.
  Pitch variations reduced to preserve the supplied voice timbre. Sounds
  remain positional and use the normal neutral mob volume slider.
- The seven custom town-character voices, Pope, generic **dialogue-screen**
  voices, dialogue timing and screen overlap logic are unchanged.
- Non-Wild NPC reaction sound behaviour is unchanged.

## Local test checklist

```powershell
cd "$HOME\CyberMods\CyberNpc"
git switch development-no-actions
git pull --ff-only
.\build-local.bat
```

Install only the new `CyberNpc.jar` in a backed-up Forge test pack.
Spawn male and female Wild NPCs and wait for ambient grunts, then trigger
reactions and damage; there should be no Piglin sound for Wild NPCs.
Use `/give @s cybernpc:island_rift` as operator and place it.
Check its texture, low-key particles, ambient sound and collision. Right-click
as a normal player: **nothing should happen**, including no discovery message
or new advancement. It must not teleport or affect any NPCs.

Build not verified in this chat: report local Gradle errors for fixing.

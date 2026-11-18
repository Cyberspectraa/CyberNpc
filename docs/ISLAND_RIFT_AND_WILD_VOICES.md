# Island Rift foundation and Wild NPC voices — CyberNpc 0.44.86

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
- Right-click as any player to **discover** the Rift. The server stores that
  player's UUID, first discovery dimension, block position and game time in
  `cybernpc_rift_discovery.dat` (overworld SavedData).
- Discovery is persistent per-player and safe across dimensions and respawns.
  First discovery shows an announcement; later interactions indicate that
  the Rift is known.
- Unbreakable in survival by default. An operator can replace the block with
  a command. This is NOT a teleport portal yet.
- No quest changes, NPC escorts, disappearing NPCs, world deletion or save
  migration in this foundation stage. Later story/escort stages should reuse
  `RiftDiscoverySavedData.hasDiscovered(ServerPlayer)` instead of inventing
  duplicate flags.

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
Use `/give @s cybernpc:island_rift` as operator, place it, and right-click
with a normal player. Rejoin to confirm the first-discovery notification
does not repeat. The Rift **must not** transport anyone yet.

Build not verified in this chat: report local Gradle errors for fixing.

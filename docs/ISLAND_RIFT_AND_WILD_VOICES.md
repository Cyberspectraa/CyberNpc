# Island Rift visual update — CyberNpc 0.44.88

## What changed

- The existing registry ID `cybernpc:island_rift` is unchanged.
- One block wide, **two blocks tall**; admin-only item placement creates both
  halves, with facing synchronized. Both disappear together if removed using
  commands. No naturally generated structure or recipe.
- Pixel-art texture depicts a narrow jagged *tear in reality*, not a pedestal
  or framed portal. Dark void centre; violet/cyan fracture edges.
- Double-height cutout block models; no Minecraft-style solid stone frame.
- **Photon is mandatory** for CyberNpc and provides the animated Rift effects.
  The project uses the same Forge 1.20.1 / Photon .fx exporter approach as
  CyberClasses and the same server-command anchoring as CyberServer.
- `assets/cybernpc/fx/island_rift.fx` is generated **into the JAR at build
  time**; no separate resource pack needed.
- Loaded Rift lower-half block entities refresh the ambient Photon animation
  every 96 ticks **only when a player is within 28 blocks**. This avoids
  network/particle work for deserted Rift locations.
- Occasional quiet ambient sound is retained. Vanilla portal particles were
  removed from the Rift; Photon now owns its animated FX.
- The Rift **DOES NOT** teleport, save discovery, start quests, activate NPC
  following/departures, or do anything when right-clicked.
- Wild NPC male/female voice recordings from 0.44.87 are untouched.

## Important compatibility rules

- Keep CyberNpc as both client+server code: model rendering and cutout render
  layers must load on clients, while block placement/FX triggers are
  server-authoritative. Do not move all code into the server-only CyberServer
  module or introduce circular mod dependencies.
- Photon is required in both Minecraft client and dedicated server modpacks.
- Never delete the island_rift block registry ID; installed test worlds might
  already contain it.
- Old single-block Rifts in test worlds will default to the lower half;
  **replace them using the updated admin item** to produce the 2-block visual.
- Do not replace or delete user gameplay data outside this visual update.

## Local build/test

```powershell
cd "$HOME\CyberMods\CyberNpc"
git switch development-no-actions
git pull --ff-only
.\build-local.bat
```

First build may download pinned Photon/LDLib compile-time dependencies.
A successful local build creates `CyberNpc.jar`; this branch does not use
GitHub Actions. Do not treat the new version as tested until compiled and
verified in a backed-up test world.

In Minecraft: `/give @s cybernpc:island_rift` as an operator, place where
there are **two free blocks vertically**, observe transparent 1x2 space-tear
with Photon motes from both sides. It should be completely inactive on right
click or walking through. Test destroying the bottom half with `/setblock`
in creative/admin mode: its top should disappear. Try dedicated multiplayer
with Photon installed on both sides, and check build-local.log if the build
fails. The 7 town character voices remain unchanged.

## Architecture decision still pending

Moving all implementation to CyberServer would force a large dependency
restructure. Keep per-mod client renderers, textures, and feature-specific
server logic now. If common code genuinely repeats, factor only that code
into a small independent shared API/module later. CyberServer stays a
server-side world director.

# CyberNpc 0.45.7 — integrated summoning effect and intro polish

## What changes

The uploaded `CyberServer-Resources.zip` (pack_format 15) contains exactly
one runtime asset: `assets/cyberserver/fx/summoning.fx` (2,714 bytes,
SHA-256 `5886a27d7d6c0b4bed7ac6568e0a7e4be3b821652f85930b33f1035eb9d3b7f0`).
It was a standalone client resource pack solely because **CyberServer is a
server-only mod**. Sending that server-only JAR to players would not make its
Photon FX available on clients.

That original binary is now embedded, without changing its ID or emitter
settings, in the client-installed **CyberNpc** JAR at:

    assets/cyberserver/fx/summoning.fx

Minecraft/Forge mounts assets from the client-installed mod JAR; Photon
continues to resolve the same stable ID `cyberserver:summoning`, while
CyberServer's existing `PhotonBridge` triggers it over the network. **No
`CyberServer-Resources.zip` installation is necessary** when the new CyberNpc
JAR is installed. Photon itself is still required, as before.

A full world/intro rewrite is intentionally avoided. The accepted first-arrival
queue, timed 45-tick title + 130-tick summoning, camera path, creator workflow,
Pope navigation/dialogue, player invisibility/reveal and escape/skip behaviour
are preserved.

## Intro screen consistency with outro

- Keep the author's actual Blockbench Fractured Island title sprite and its
  already-correct UV facing; do not invent replacement art.
- Use the same neutral background, subtle one-pixel/two-pixel borders,
  parchment/accent colour and vanilla pixel font as the Rift farewell.
- Keep the title **stationary**, fading rather than scaling up each frame.
- Include one small "A CYBERSPECTRA SERIES" line beneath the title,
  not a duplicated floating title footer or overly cinematic copy.
- Remove heavy 10-pixel dark purple bars from the world view. Use thin
  pixel rules only; let the real 3D world and Photon beam supply the visuals.
- Reduce the harsh reveal flash and tint it a muted warm gold, matching
  the existing golden summoning effect.

## Compatibility and verification

CyberServer remains a server-only world director and still starts
`cyberserver:summoning` as before. This fixes asset distribution, not
entity behaviour; no NPC registry IDs, dialogue, quests, races, classes,
Rift state, saved data or network messages are changed.

After a successful local build (Forge 1.20.1 / Java 17), inspect
`CyberNpc.jar` as a ZIP/JAR: it should contain
`assets/cyberserver/fx/summoning.fx` with the SHA-256 listed above. The
original `CyberServer-Resources.zip` should **not** be installed in the
Minecraft resource packs folder; disabling it is a useful regression test.

Suggested tests:
1. With CyberNpc 0.45.7 on both sides, CyberServer 0.2.6 on the dedicated
   server and the existing Photon dependency, replay a full intro with
   `/arrival test`. The golden summoning rings, wisps, long beam, shockwave,
   and arrival burst should appear without the external resource ZIP.
2. Confirm the title artwork is correctly oriented and readable at GUI
   scale 2 and 4, and that the intro's fade and bottom border match the
   restrained Rift outro rather than a different neon UI theme.
3. Skip the intro and reconnect during the sequence. Verify the player is
   not left invisible/floating and the Pope dialogue still starts normally.
4. Check that the intro, Rift, CyberQuest and CyberClasses continue to work
   in multiplayer with both clients using the matching CyberNpc version.

The source was committed to `development-no-actions`. Do not treat the
change as compiled, tested or shipped until the owner's local
`build-local.bat` succeeds and gameplay is verified. Do not trigger GitHub
Actions.

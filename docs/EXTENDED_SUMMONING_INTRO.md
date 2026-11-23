# Fractured Island — extended summoning intro (CyberNpc 0.45.8)

CyberNpc keeps the established authored Blockbench title, original
`cyberserver:summoning` Photon FX bundled into its own client JAR, and the
existing camera/queue/Pope state machine. No separate resource pack or
additional mod is needed beyond the existing optional CyberServer and Photon.

## Timing

The old 45-tick title + 130-tick summoning = **8.75 seconds**.
This update uses 70 ticks + 190 ticks = **13 seconds** at 20 TPS.

| Scene time | Server tick | What happens |
|---|---:|---|
| 0–3.5s | 0–70 | Stable owner's title artwork with a longer fade/hold |
| 3.5s | 70 | Original CyberServer golden Photon ritual + sounds begin |
| 8.2s | 164 | A second, softer pulse replays the bundled Photon effect without repeating the whole arrival event and its sounds |
| 10.6s | 212 | Player is made visible at the height of the second pulse; existing gold/End Rod particle reveal remains |
| 13s | 260 | Continuous camera arc finishes and Pope greeting handoff begins |

The second pulse uses the existing optional `PhotonBridge.play` by
reflection. If CyberServer or Photon is unavailable, CyberNpc produces a
small vanilla particle/sound beat instead. The first pulse also has a
vanilla fallback when CyberServer is absent. No extra packets or saved data
fields are added.

The intro waiting screen now uses the same vanilla Minecraft font, restrained
parchment palette and thin borders as the title and Rift finale, rather than
the old purple modern-style panel.

## Important behaviour retained

- Player character creator and class-registration flow unchanged
- Single-player-at-a-time FIFO summoning queue unchanged
- Existing fixed camera / raycast-protected moving arc unchanged; no jump cuts
- Character invisibility, invulnerability and gravity safety remain
  server-authoritative, with reveal restored before the greeting
- Escape/skip behaviour, interrupted-session recovery, dialogue and NPC
  pathfinding/selection are untouched
- Rift finale and all other NPC systems unchanged

## Manual regression tests

Build locally on Java 17. Do not use GitHub Actions. Install the new
CyberNpc JAR on both Forge 1.20.1 clients and server.

1. Use `/arrival test` with the CyberServer intro markers configured.
   Check that the title stays readable and in its authored orientation.
2. Check the two effect beats; the second should visibly bridge the gap
   between the first beam and the player's appearance, without an entire
   second CyberServer first-arrival sequence firing.
3. Verify no separate `CyberServer-Resources.zip` is necessary with
   CyberNpc 0.45.8 and the optional Photon mod installed.
4. Test with no CyberServer: both timed beats use native fallback effects,
   and the scene must still complete without trapping the player.
5. Test ESC skip, reconnect, multiplayer queued arrivals, Pope greeting,
   GUI scale 2 and 4, and the normal Rift finale.
6. If the server advances under load, the scene still finishes after the
   server-owned deadline and the 35-tick grace period; it must not hang.

Source committed to the `development-no-actions` branch; no compilation
or in-game verification was possible from this environment.

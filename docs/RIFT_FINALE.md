# Rift finale — CyberNpc 0.44.99 (development-no-actions)

The owner's 20 November 2026 direction is now an **opt-in series outro** on
`cybernpc:island_rift`. This replaces the Rift's prior gameplay-inactive rule
without changing its block/item ID, two-block model, placement, textures,
no-collision behaviour or Photon FX. Do not move this system into CyberServer.

## Player flow

1. Walking *through* either half of an intact, 1×2 Rift triggers a server-issued
   prompt: **THE FINAL CROSSING**. Just touching a Rift cannot cause departure.
2. The player chooses **Stay on the Island** (or Escape) and retains all
   control/progress. They will not be repeatedly prompted until they leave the
   Rift and walk back into it.
3. **Enter the Rift** is validated server-side against a live 30-second offer,
   the current dimension, Rift integrity and player proximity. After acceptance,
   the player becomes invisible, invulnerable and fixed in place while a private
   42-second credits sequence plays. The Rift is NOT a teleport.
4. Credits display: farewell title; *Minecraft's own* lifetime play time,
   mob kills and deaths; the Main Seven's total visits, most frequently talked
   to character and runner-up; count of established trusted relationships;
   thank-you; a deliberately non-specific next-series teaser. All counters
   derive from actual persistent player data or vanilla stats, not simulation.
5. At the end, CyberNpc saves `CyberNpcRiftDeparture` on the player and
   disconnects them with a farewell reason. On dedicated servers, later logins
   are refused: this is the requested **no turning back**.
6. **Integrated singleplayer exception:** the owner can re-open the world for
   testing after being disconnected. The departure remains marked and the Rift
   will not play again without an admin reset. Do not lock the only world owner
   out of their save.

## Interrupted outro and recovery

- Acceptance immediately persists `CreditsInProgress`. If the client crashes,
  logs out or the server restarts before the credits end, the credits replay on
  the next login (after a brief world-loading delay). The player cannot opt out
  of their accepted departure by disconnecting.
- The session restores the player invisibility, gravity and invulnerability
  flags it found at the start. Skip/decline never changes these flags.
- Operator command **`/cybernpc rift reset`** clears the executor's saved
  departure (suitable for integrated/local testing).
- On a dedicated server, the console or another operator can run
  **`/cybernpc rift recover <player UUID>`**, granting that offline player
  a one-time reset on their *next* login. This pending permission is stored in
  server memory; ask them to rejoin before restarting the server.
- `PlayerEvent.Clone` preserves departure state on character clone/death.
  Dedicated servers should back up test saves before a final irreversible choice.

## Scope of statistics

- Named NPC conversation counts are **existing** `Visits` fields stored under
  `CyberNpcTownCharacters.<storyId>`, including older conversations. Kai's
  existing stable story ID remains `mason`.
- Only the Main Seven (Kai, Katie, Elara, Rowan, Felix, Nadia, Orin) have these
  saved per-character counts. Pope, Guild Receptionist and wild speech bubbles
  are *not* counted in that leaderboard.
- Trusted connections use the existing `Trust >= 2 && Affection >= 1`
  values. They are a recap, not an NPC escort or migration promise.
- The credits do not claim completed quest statistics until a stable CyberQuest
  summary API exists. The current persistent Rift record stores the snapshot
  (highest-visit NPC, time, kills, deaths and number of trusted relationships),
  while existing relationships/quest progress remain in their owning mods.

## Manual test checklist

1. On `development-no-actions`, run `git pull --ff-only` and
   `build-local.bat`, replace the old JAR and confirm a **real build success**.
   Forge 1.20.1 / Java 17, mandatory Photon, and **matching CyberNpc 0.44.99
   on client and server** are required. Network protocol is now `8`.
2. Walk into both Rift halves: confirm the warning appears once, neither side
   automatically accepts. Escape/Stay must NOT alter your character.
3. Leave its block and walk back; warning should reappear only after re-entry.
   Stand near (but not inside) the Rift; it must never trigger.
4. Accept once. Check player becomes invisible, cannot move, and receives
   complete, readable credits with genuine recorded stats.
5. Let all 42 seconds finish; confirm the disconnect reason. In a
   multiplayer test, rejoin and confirm server refuses a departed player.
6. Operator reset and confirm the outro is testable again. If using a dedicated
   server, check offline UUID recovery.
7. Disconnect mid-credits, reconnect and confirm it restarts the credits.
8. Verify that Photon plays normally, intro/Pope dialogues still work and
   other NPC features/quest IDs are unaffected.

## Not implemented by design

There is no world generation, portal teleport, next-series destination
dimension, quest completion gate, transfer/export of the Main Seven, or auto-
escort. Those are separate world/story systems requiring approval and a clear
cross-project data format. The outro text is intentionally a teaser, not
unapproved canon about the destination. No GitHub Actions builds should run.

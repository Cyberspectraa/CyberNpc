# Rift finale — CyberNpc 0.45.4 (development-no-actions)

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
3. **Enter the Rift** is validated server-side against a live 90-second offer,
   original arrival dimension and server-issued invitation. The Rift is
   verified before the warning is offered; its block state and the player's
   distance are intentionally not rechecked after they confirm. After acceptance,
   the player becomes invisible, invulnerable and fixed in place while a private
   72-second credits sequence plays. The Rift is NOT a teleport.
4. Credits display: farewell title; *Minecraft's own* lifetime play time,
   mob kills and deaths; the Main Seven's total visits, most frequently talked
   to character and runner-up; count of established trusted relationships;
   thank-you; acknowledgements for the requested community resources and tools;
   a deliberately non-specific next-series teaser. All counters
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
   Forge 1.20.1 / Java 17, mandatory Photon, and **matching CyberNpc 0.45.4
   on client and server** are required. Network protocol is now `9`.
2. Walk into both Rift halves: confirm the warning appears once, neither side
   automatically accepts. Escape/Stay must NOT alter your character.
3. Leave its block and walk back; warning should reappear only after re-entry.
   Stand near (but not inside) the Rift; it must never trigger.
4. Accept once. Check player becomes invisible, cannot move, and receives
   complete, readable credits with genuine recorded stats, acknowledgements
   and the uploaded AtlasAudio music. UI must say 'The Rift is opening...' until
   the server sends the credits packet, never silently close or repeat.
5. Let all 72 seconds finish; confirm the disconnect reason. In a
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

## 0.45.0 fix: confirmation and music

The previous release cleared its pending prompt every five ticks whenever the
player was no longer intersecting a Rift block. Movement can settle just
outside the narrow tear as the warning opens, so a delayed click on **Enter**
was refused without any user feedback. **Now** the offer stays valid until its
90-second deadline. The server validates the original single-use invitation, expiry, dimension,
and departure flags. The full Rift is verified when the prompt is first offered;
it is no longer rechecked at acceptance time because the live block state can
change during the non-pausing warning. Players may naturally drift or fall. If it
rejects the request, the client receives an explicit explanation rather than
sitting on a silent warning. Clicking Enter shows **The Rift is opening...**
while awaiting authoritative server acknowledgement; Escape/Stay always declines.

### Soundtrack

- Supplied file: `atlasaudio-background-music-635570.mp3` (128.6 seconds).
- Mod asset: `assets/cybernpc/sounds/music/rift_outro.ogg` — 72 seconds,
  stereo Ogg Vorbis at 44.1 kHz, faded in over 2 seconds and out over 5.
- Sound event: `cybernpc:rift_outro_music` is streamed in `sounds.json`.
  The credits screen plays it on the Minecraft music channel and stops it
  on completion, server abort or screen removal. Credits length is 72 seconds.
- Attribution on the credits page: **AtlasAudio (via Pixabay)**, based on the
  file's supplied name. Keep a copy of the exact source URL/licence when the
  pack is publicly distributed; the metadata in the MP3 did not include it.

### Credits acknowledgements (as requested by the owner)

Fresh Animations; Fresh Animations: Player Extension; Just Expressions;
Voice Studio; StarlightSkins; Pixabay; Blockbench;
Epic Knights: Shields, Armor and Weapons; Skin Layers 3D; Photon;
Iron's Spells 'n Spellbooks. These are acknowledgements of third-party
resources/tools, **not** a statement that they sponsor CyberNpc or all use the
same license. The owner should check each asset's redistribution terms before
pack release.

**Source and OGG binary are committed on `development-no-actions`; no compiled
JAR or in-game test has been verified. No GitHub Actions were run.**

## 0.45.1 fix: player fell away while reading

A valid warning could display but **Enter the Rift** responded “The Rift is too
far away.” The issue was the second distance-to-Rift check, not the original
physical contact: the tear has no collision and the non-pausing warning lets
the player's physics continue, carrying them more than three blocks from the
Rift before clicking. The 0.45.1 server now trusts the *one-use prompt issued
at touch*, provided it is less than **90 seconds** old, the same complete Rift
still exists in the same dimension, the player explicitly chooses Enter and
has not already departed. Fake Yes packets without such a prompt still fail.
The intro, soundtrack, credits and disconnect behaviour are unchanged.

Test specifically with the Rift suspended above the ground: open the warning,
let the player fall while reading, then choose Enter. The real credits screen
should replace the warning. Test Stay and Escape separately; neither should
start the ending. Confirm both server and client are using 0.45.1.

## 0.45.2 fix: do not revalidate Rift blocks after the invitation

The 0.45.1 **"The Rift is no longer available"** feedback was generated by one
combined gate that rechecked both the player's dimension and the Rift's two
block states when they pressed Enter. The warning itself can only be issued
by `RiftFinaleService.touch()`, which *already verified* the complete server
Rift and physically intersecting player. Rechecking live block state later
can reject a legitimate invitation if the world changes while the GUI is open.

**0.45.2 uses the original server-issued one-use invitation as the source of
truth**. On Enter, validate the saved invitation and its 90-second deadline,
that the player remains in the original dimension, and that this player has not
already departed or begun the intro. Do not inspect Rift blocks or distance
again. A distinct message identifies actual dimension changes; stale or missing
prompts still fail securely. The original visual Rift and Photon FX are untouched.

Regression test: walk into an intact Rift to open the warning, move or fall
away while the UI remains open, then choose Enter. The server should transition
directly into the credits with AtlasAudio music. Press Stay/Escape in another
run to verify no departure or lock. The credits/end disconnect must still work.

This is a GitHub source patch; local build and game testing remain necessary.

## 0.45.3 credit presentation

No gameplay or network changes. The **72-second** server-owned outro and streamed
AtlasAudio music are preserved exactly. The client credits renderer uses:

- A plain, restrained Minecraft-font screen with two thin pixel lines instead
  of the old moving fake-rift particles and thick purple ornament.
- Static headings, statistics and names. No per-frame zoom or positional drift.
- 24-tick C1-smoothstep crossfades overlapping at page boundaries, so a card
  fades into the next instead of vanishing abruptly.
- Static reusable entries (no per-render arrays); automatic width fitting for
  exceptionally long project titles.
- Eight cards in 72 seconds: title (0–7s), personal stats (7–16s), town NPCs
  (16–25s), thank-you (25–31s), resource packs (31–42s), mods (42–53s),
  tools/websites/creators (53–62s), and next-series tease (62–72s).

### Exact credit categories

| Credited resource | On-screen description |
|---|---|
| Fresh Animations | Resource pack - mob animations |
| Fresh Animations: Player Extension | Resource pack - player animations |
| Just Expressions | Resource pack - player expressions |
| Epic Knights: Shields, Armor and Weapons | Mod - medieval equipment |
| Skin Layers 3D | Mod - 3D player skins |
| Photon | Mod - visual effects editor |
| Iron's Spells 'n Spellbooks | Mod - spells and magic |
| Blockbench | Tool - 3D modelling |
| Voice Studio | Tool - voice / audio |
| StarlightSkins | Minecraft skin creator |
| Pixabay | Website - stock media |
| AtlasAudio | Outro music - via Pixabay |

StarlightSkins is classified as a skin creator, not assumed to be a mod or
website; "Voice Studio" is deliberately described broadly without assuming
a particular product URL. The music credit is separate from the website credit.

The changes are confined to `RiftCreditsScreen.java` plus mod version and
documentation. Build locally on Java 17/Forge 1.20.1 and check line fitting,
fade smoothness, credit readability at both GUI scale 2 and GUI scale 4,
music continuity and the final disconnect. Do **not** run GitHub Actions.

## 0.45.4 — Final crossing UI and traditional credits roll

This is a **presentation-only** change. The server-owned 72-second duration,
Rift validation and entry confirmation, saved stats, player invisibility,
sound event and OGG file, disconnect and recovery are unchanged. No other Cyber
mods are modified. Minecraft font and default Minecraft buttons are retained.

### Why the previous transition still felt choppy

0.45.3 drew the old and new page on top of each other with 24-tick overlapping
crossfades. When a heading and several lines occupied roughly the same screen
position, they appeared doubled during the fade. Increasing smoothing alone
does not correct the problem.

0.45.4 replaces that with three deliberately simple phases:
1. **Quiet opening and recap cards** (0–30 seconds): title, playtime and kills,
   NPC visit/relationship statistics, then an actual thank-you. Each card
   fades completely out before the next one appears. The 18-tick eased fade
   and six-tick half-gap leave a small dark pause on each side of the cut.
   No text overlays, positional changes or zooms.
2. **Single continuous scrolling credits roll** (30–62 seconds): resource packs,
   mods, tools/websites and music. All named resources retain concise type
   descriptions. Scrolling uses render partial ticks and fractional pose
   translation for smooth, steady motion at different frame rates, and clips
   to the central viewing area. No sudden list swap.
3. **Beyond the Rift** (62–72 seconds): a short restrained teaser that fades in
   after the last credits scroll away.

### Shared in-game style

Both `RiftDecisionScreen` and `RiftCreditsScreen` now use the small shared
`RiftUiStyle` palette: dark neutral background, two thin warm-grey pixel
borders, vanilla Minecraft font, muted parchment headings, pale body text.
No glowing purple panel, animated stars, particle backdrop, unnecessary
colourful decoration or per-frame font resizing. The warning has ordinary
Minecraft buttons ('Stay on the Island' and 'Enter the Rift'), explicitly
says the decision is permanent and explains that credits play before the
disconnect. All on-screen behaviour is still server-authoritative and
explicitly opt-in.

### Regression checks for the local 0.45.4 build

- Enter the Rift in both survival and spectator (spectator must never
  start an ending); **Stay** and Escape must leave the player on the island.
- **Enter** must lead from warning to actual credits without a distance or
  block-state rejection. The previous 0.45.2 fix is not modified.
- Watch at different Minecraft GUI scales, including 2 and 4: no overlapping
  cards, clipped names, shimmering text, fast jumps or unwanted scroll stops.
- All supplied credit names and types must appear during the single roll.
- Outro music should play continuously, stop on disconnect/reset, and
  last the same 72 seconds as before.
- Final disconnect, recorded departure and admin reset must still function.

GitHub source was changed on `development-no-actions`; the branch must be
built locally with Java 17 / `build-local.bat`. Do not trigger GitHub Actions.

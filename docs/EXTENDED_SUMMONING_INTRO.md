# Fractured Island — single-beam extended intro (CyberNpc 0.45.14)

The earlier 0.45.8 extended intro replayed the same Photon effect twice
(at ticks 70 and 164) but only revealed the player at tick 212. This
made the first beam look like a failed summon. The scene still lasts
260 ticks (13 seconds at 20 TPS); the original title, camera, queue,
Pope greeting and skip/reconnect behaviour are retained.

| Scene time | Tick | Behaviour |
| --- | ---: | --- |
| 0–3.5s | 0–70 | Original title artwork |
| 3.5–7.5s | 70–150 | Sparse floor-level vanilla particles, no beam |
| 7.5s | 150 | One original CyberServer golden summoning effect and sounds |
| 10.6s | 212 | Reveal the actual player; existing particle flash |
| 13s | 260 | Camera handoff and Pope greeting |

The original 0.45.7 beam-to-reveal delay was 62 ticks. The new timing
preserves that delay while retaining the extended scene length. There
is no second Photon command or second arrival sequence. If CyberServer
or Photon is absent, the existing vanilla fallback still runs.
No asset or network protocol change is needed.

## Test

Install matching CyberNpc 0.45.14 on client and server.
Run /cadmin arrival test with the intro markers configured.
Check for one beam, a visible character before the camera handoff,
and the Pope's greeting. Test a fresh character separately, then ESC
skip and queued multiplayer arrivals. The same server-owned tick
deadlines and grace period apply. Source checks do not establish
successful Forge compilation or visual synchronization under lag.

# CyberNpc voice-grunt system — 0.44.84

This update completes the first VoiceStudio voice pack for all NPCs currently using scripted dialogue. Kai and Katie retain their previously tested recordings and exact timing. The other eight profiles now use the supplied VoiceStudio WAV recordings, not synthetic placeholder audio.

## Voice sets

Each character has three short OGG files in `assets/cybernpc/sounds/dialogue/`, named `<voice>_hm.ogg`, `<voice>_mm.ogg`, and `<voice>_uh.ogg`.

- `kai`: Kai (persisted story ID `mason`; left unchanged).
- `katie`: Katie (persisted story ID `town_npc_1`; left unchanged).
- `elara`: Elara (`town_npc_2`).
- `rowan`: Rowan (`town_npc_3`).
- `felix`: Felix (`town_npc_4`).
- `nadia`: Nadia (`town_npc_5`).
- `orin`: Orin (`town_npc_6`).
- `pope`: Pope (service role).
- `male` and `female`: all other CyberNpc characters with dialogue, chosen from NPC appearance gender; each entity has a stable small pitch variation.

The sound event registry IDs (e.g. `cybernpc:dialogue_voice_elara`) are unchanged, preserving resource-pack compatibility for sound events.

## Recording preparation

The 24 new recordings were analysed at their original 24 kHz rate. Silent introductions, trailing silence and obvious airy/non-voiced sections were trimmed around the identifiable voiced/grunt part; a gentle low-frequency cut, high-frequency rolloff, short fades and conservative peak/RMS normalisation prevent clicks and big loudness differences. Samples were converted to **22,050 Hz mono OGG Vorbis**. New clips are approximately 96–165 ms each. The original WAVs remain in the user's uploaded ZIP; the six Kai/Katie assets remain byte-for-byte unchanged.

## Playback rules

- Voice grunts are **always active** without a separate configuration file. Minecraft's normal sound sliders still work.
- Only the speaking NPC produces grunts; the player's own text remains silent.
- At most one short NPC grunt can play at once: triggering a new sound stops the previous one, and closing or changing the dialogue screen also stops it.
- The system is driven by the dialogue screen's existing text reveal, with no AI, tick cost on NPC entities, or server-side network traffic.
- Kai and Katie remain at their tested 2-letter and 95/90 ms trigger cadence, 1.00 pitch, 1.00 gain, with a 0.60 base volume.
- The other voices now use the same responsive 2-letter cadence with a character-specific minimum interval of about 90–120 ms, 1.00 pitch and gain, and the same 0.60 base volume. Stable generic-entity pitch variation remains for individuality.
- Clicking to finish text does not burst grunts. **Manual click-to-continue is unchanged.**

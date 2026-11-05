# CyberNpc dialogue voice grunts

This update gives **every speaking CyberNpc dialogue** short, non-verbal typewriter sounds. It does **not** generate TTS or use AI at runtime, and the player character's typed response stays silent. The dialogue still waits for a manual click to advance.

The first build includes deliberately simple, original synthetic preview grunts. They are not VoiceStudio recordings; replace them with your VoiceStudio exports later. No third-party character recordings were used.

## Character filenames

All voice samples live in `assets/cybernpc/sounds/dialogue/` inside the mod or an overriding client resource pack.

**Kai:**
- `kai_hm.ogg`, `kai_mm.ogg`, `kai_uh.ogg`

**Katie:**
- `katie_hm.ogg`, `katie_mm.ogg`, `katie_uh.ogg`

The six Kai/Katie clips now come from the user's second, shortened VoiceStudio M4A recordings. They are precisely onset-trimmed to remove AAC/recording lead-in, capped at roughly 65–125 ms of voiced material, cleaned with very gentle low-frequency filtering, given a short fade, and peak-safe volume balanced. They are encoded as 22,050 Hz mono OGG Vorbis.

Minecraft chooses randomly between the three variants for each character. Kai and Katie now use original pitch (1.0), with only about 1% subtle variation, and have faster character-synchronised timing (2 revealed characters, 95/90 ms minimum gap). If a frame ends on punctuation or whitespace, the last newly revealed letter can still trigger a grunt. Manual click-to-continue is unchanged.

The remaining placeholder voices are unchanged for now:
- `elara.ogg`, `rowan.ogg`, `felix.ogg`, `nadia.ogg`, `orin.ogg`
- `pope.ogg`, `male.ogg`, `female.ogg`

To replace Kai or Katie's voice later, overwrite the matching three files in a resource pack, or change their three entries in `assets/cybernpc/sounds.json`. No source-code changes are needed for ordinary audio replacements.

The receptionist, and any future NPCs with text dialogue, automatically use their appearance's male/female default, with a stable small pitch variation per entity. Named NPCs use their own sounds regardless of randomly generated appearance so their voices stay consistent.

### Making clips in VoiceStudio

Record or synthesize a short original syllable such as "mm", "uh", or "hm" in VoiceStudio and export WAV. Trim the beginning/end silence and convert to OGG Vorbis, e.g. `ffmpeg -i kai.wav -ac 1 -ar 22050 -c:a libvorbis -q:a 3 kai.ogg`. Do not make clips full sentences, since the game uses them as tiny voice grunts while letters appear. A few different syllables per voice can be layered into `sounds.json` later, without changing Java.

For recordings based on real people's voices, use recordings with their permission. Check the selected VoiceStudio model's license before releasing generated audio; the application and model licenses differ.

## Playback

Voice grunts are always active for NPCs with dialogue. There is no `cybernpc-dialogue-client.toml` file or separate voice-grunt toggle. The fixed base volume is 0.60 (before the individual NPC gain); Kai and Katie use gain 1.0 to make their recorded syllables clearer. Minecraft's standard sound sliders still apply.

The sound only plays while the NPC's line is typing; it stops immediately when the line is clicked to finish, or the conversation closes. Sound playback is local and adds no server-side AI or network traffic.

## 0.44.83 playback cleanup

The book-page sound is no longer played when the dialogue UI opens or switches to another response. For Kai and Katie, a newly triggered syllable **replaces** the previously playing syllable instead of overlapping it; the currently active clip is also stopped when the dialogue screen is removed. This is client-only and does not change any NPC dialog, authored voice recordings, the 2-character/95–90ms trigger frequency, the pitch, or the 0.60 base volume. The original manual click-to-continue remains.

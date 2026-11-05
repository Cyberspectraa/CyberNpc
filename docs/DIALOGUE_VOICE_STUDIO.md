# CyberNpc dialogue voice grunts

This update gives **every speaking CyberNpc dialogue** short, non-verbal typewriter sounds. It does **not** generate TTS or use AI at runtime, and the player character's typed response stays silent. The dialogue still waits for a manual click to advance.

The first build includes deliberately simple, original synthetic preview grunts. They are not VoiceStudio recordings; replace them with your VoiceStudio exports later. No third-party character recordings were used.

## Character filenames

Put your VoiceStudio renders in a client resource pack at the following paths (each 0.06–0.14-second OGG, mono, no silence):

- assets/cybernpc/sounds/dialogue/kai.ogg
- assets/cybernpc/sounds/dialogue/katie.ogg
- assets/cybernpc/sounds/dialogue/elara.ogg
- assets/cybernpc/sounds/dialogue/rowan.ogg
- assets/cybernpc/sounds/dialogue/felix.ogg
- assets/cybernpc/sounds/dialogue/nadia.ogg
- assets/cybernpc/sounds/dialogue/orin.ogg
- assets/cybernpc/sounds/dialogue/pope.ogg
- assets/cybernpc/sounds/dialogue/male.ogg
- assets/cybernpc/sounds/dialogue/female.ogg

The receptionist, and any future NPCs with text dialogue, automatically use their appearance's male/female default, with a stable small pitch variation per entity. Named NPCs use their own sounds regardless of randomly generated appearance so their voices stay consistent.

### Making clips in VoiceStudio

Record or synthesize a short original syllable such as "mm", "uh", or "hm" in VoiceStudio and export WAV. Trim the beginning/end silence and convert to OGG Vorbis, e.g. `ffmpeg -i kai.wav -ac 1 -ar 22050 -c:a libvorbis -q:a 3 kai.ogg`. Do not make clips full sentences, since the game uses them as tiny voice grunts while letters appear. A few different syllables per voice can be layered into `sounds.json` later, without changing Java.

For recordings based on real people's voices, use recordings with their permission. Check the selected VoiceStudio model's license before releasing generated audio; the application and model licenses differ.

## Playback

Voice grunts are always active for NPCs with dialogue. There is no `cybernpc-dialogue-client.toml` file or separate voice-grunt toggle. The fixed, quiet base volume is 0.30 (before the individual NPC gain). Minecraft's standard sound sliders still apply.

The sound only plays while the NPC's line is typing; it stops immediately when the line is clicked to finish, or the conversation closes. Sound playback is local and adds no server-side AI or network traffic.
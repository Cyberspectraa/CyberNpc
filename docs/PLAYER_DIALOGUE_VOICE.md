# Player reply voices — CyberNpc 0.44.90

The local player's chosen dialogue replies now make the same short
VoiceStudio-style syllable/grunt sounds already used by named NPCs.

## Sound design
- Triggered **only while the player's selected reply types out**, with a
  similar 2-letter and 100–105 ms cadence to NPCs. NPC grunts are untouched.
- Local client audio only; no voice recording, speech recognition, TTS,
  network messages, entity ticks or server-side audio.
- Each grunt stops the previous one, and screen/page changes stop the last.
- Clicking to reveal all text remains silent; selecting Farewell or Skip
  does not generate a voice line.
- New resource-pack-overridable sound events:
  `cybernpc:dialogue_voice_player_low`,
  `cybernpc:dialogue_voice_player_high`.
  Both initially reuse existing supplied generic dialogue voice clips, so
  no invented new samples are bundled. Replace these event definitions in a
  resource pack if player-specific recordings become available.

## Player preference
A client-only Forge config is registered in
`config/cybernpc-dialogue-client.toml`:

```toml
[dialogue]
    player_voice = "LOW"
    player_voice_volume = 0.45
```

Choose `LOW`, `HIGH` or `OFF` for the player's own reply blips.
`LOW` and `HIGH` select different existing generic voice recordings.
The settings do not infer voice from race, appearance or player identity,
and do not affect NPC voices or multiplayer gameplay state.
Minecraft's normal sound sliders continue to apply.

## Local test
1. Build locally using `build-local.bat` and replace the older CyberNpc JAR.
2. Open the Pope dialogue, choose a non-exit reply, and watch the player
   portrait and typed text: player grunt sounds should accompany the reply
   rather than playing only when the Pope speaks.
3. Click once to skip typing: no sudden burst of sound; click again to display
   the queued Pope response with his original voice.
4. Test a longer Kai/Katie response, then a Guild Receptionist response,
   including the class UI handoff. Quest actions and dialogue timing unchanged.
5. In the client's config, try `HIGH` and `OFF`; confirm NPC voice blips
   continue even if local player replies are muted.
6. Test a dedicated server: no player audio packets are needed. The server
   still validates every response as before.

**Code committed for local build; not compiled or Minecraft-tested by assistant.**

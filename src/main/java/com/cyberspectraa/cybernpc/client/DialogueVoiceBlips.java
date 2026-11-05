package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.dialogue.DialogueView;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcAppearance;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Character-specific Undertale-style voice grunts. All decisions are client-side
 * and tied to the already-visible typewriter animation; never run NPC ticks.
 * Uses sound events so VoiceStudio samples can be swapped via resource packs.
 */
public final class DialogueVoiceBlips {
    private record Voice(RegistryObject<SoundEvent> sample,
                         float pitch, float gain, int letters, int minMillis) { }

    private static final Voice KAI = new Voice(ModSounds.DIALOGUE_VOICE_KAI, 1.00F, 1.00F, 2, 95);
    private static final Voice KATIE = new Voice(ModSounds.DIALOGUE_VOICE_KATIE, 1.00F, 1.00F, 2, 90);
    private static final Voice ELARA = new Voice(ModSounds.DIALOGUE_VOICE_ELARA, 1.00F, 1.00F, 2, 100);
    private static final Voice ROWAN = new Voice(ModSounds.DIALOGUE_VOICE_ROWAN, 1.00F, 1.00F, 2, 115);
    private static final Voice FELIX = new Voice(ModSounds.DIALOGUE_VOICE_FELIX, 1.00F, 1.00F, 2, 90);
    private static final Voice NADIA = new Voice(ModSounds.DIALOGUE_VOICE_NADIA, 1.00F, 1.00F, 2, 105);
    private static final Voice ORIN = new Voice(ModSounds.DIALOGUE_VOICE_ORIN, 1.00F, 1.00F, 2, 120);
    private static final Voice POPE = new Voice(ModSounds.DIALOGUE_VOICE_POPE, 1.00F, 1.00F, 2, 120);
    private static final Voice GENERIC_MALE = new Voice(ModSounds.DIALOGUE_VOICE_MALE, 1.00F, 1.00F, 2, 105);
    private static final Voice GENERIC_FEMALE = new Voice(ModSounds.DIALOGUE_VOICE_FEMALE, 1.00F, 1.00F, 2, 100);

    // Constant client-side level; no separate mod config is required.
    // Players can still use Minecraft's normal sound settings.
    private static final float DEFAULT_VOLUME = 0.60F;

    // All dialogue voices use supplied VoiceStudio clips. Keep at most one
    // short syllable active at a time to avoid layered/grainy speech.
    private static SimpleSoundInstance activeRecordedBlip;

    private DialogueVoiceBlips() { }

    /** Called when the dialogue screen closes or changes to another page. */
    public static void stopActive() {
        if (activeRecordedBlip != null) {
            Minecraft.getInstance().getSoundManager().stop(activeRecordedBlip);
            activeRecordedBlip = null;
        }
    }

    /** Returns true if a grunt actually played. Screen stores last played time/index. */
    public static boolean play(DialogueView view, String speech,
                               int previousCount, int count,
                               int lastPlayedIndex, long now, long lastPlayedMillis) {
        if (count <= previousCount || speech.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        if (!(mc.level.getEntity(view.entityId()) instanceof CyberNpcEntity npc)) return false;

        Voice voice = resolve(npc);
        if (count - lastPlayedIndex < voice.letters()
                || now - lastPlayedMillis < voice.minMillis()) return false;
        // One render frame can reveal multiple characters. If it happens to
        // finish on whitespace or punctuation, use the latest real letter in
        // this newly revealed section rather than silently skipping a grunt.
        int i = Math.min(count, speech.length()) - 1;
        int lower = Math.max(previousCount, i - 7);
        while (i >= lower && !Character.isLetterOrDigit(speech.charAt(i))) i--;
        if (i < lower) return false;

        // Vary pitch subtly between grunts, but give each NPC a consistent tone.
        int variation = (i * 31 + speech.length() * 7 + npc.getId() * 13) & 7;
        // Keep supplied recordings near their original pitch (especially Katie).
        // Preserve the timbre from the supplied recordings for all named voices.
        float pitchSpread = 0.004F;
        float pitch = voice.pitch() * (1.0F + (variation - 3.5F) * pitchSpread);
        if (isGeneric(voice)) {
            // Stable per-entity personality for future authored dialogue NPCs.
            pitch *= 1.0F + (Math.floorMod(npc.getUUID().hashCode(), 9) - 4) * 0.018F;
        }
        float volume = DEFAULT_VOLUME * voice.gain();
        SimpleSoundInstance sound = SimpleSoundInstance.forUI(
                voice.sample().get(), pitch, volume);

        // Replace the previous syllable for every speaking NPC. Kai and Katie
        // keep their existing pitch, gain and timing unchanged.
        stopActive();
        activeRecordedBlip = sound;
        mc.getSoundManager().play(sound);
        return true;
    }

    private static boolean isGeneric(Voice voice) {
        return voice == GENERIC_MALE || voice == GENERIC_FEMALE;
    }

    private static Voice resolve(CyberNpcEntity npc) {
        return switch (npc.getStoryNpcId()) {
            // The persisted story id for Kai remains 'mason' for save safety.
            case "mason" -> KAI;
            case "town_npc_1" -> KATIE;
            case "town_npc_2" -> ELARA;
            case "town_npc_3" -> ROWAN;
            case "town_npc_4" -> FELIX;
            case "town_npc_5" -> NADIA;
            case "town_npc_6" -> ORIN;
            default -> {
                if (NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.POPE) yield POPE;
                yield npc.getAppearanceGender() == NpcAppearance.Gender.FEMALE
                        ? GENERIC_FEMALE : GENERIC_MALE;
            }
        };
    }
}
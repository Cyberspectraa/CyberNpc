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

    private static final Voice KAI = new Voice(ModSounds.DIALOGUE_VOICE_KAI, 0.86F, 0.80F, 4, 105);
    private static final Voice KATIE = new Voice(ModSounds.DIALOGUE_VOICE_KATIE, 1.17F, 0.90F, 3, 85);
    private static final Voice ELARA = new Voice(ModSounds.DIALOGUE_VOICE_ELARA, 1.03F, 0.78F, 4, 100);
    private static final Voice ROWAN = new Voice(ModSounds.DIALOGUE_VOICE_ROWAN, 0.78F, 0.70F, 5, 140);
    private static final Voice FELIX = new Voice(ModSounds.DIALOGUE_VOICE_FELIX, 1.12F, 0.80F, 3, 100);
    private static final Voice NADIA = new Voice(ModSounds.DIALOGUE_VOICE_NADIA, 0.91F, 0.76F, 4, 125);
    private static final Voice ORIN = new Voice(ModSounds.DIALOGUE_VOICE_ORIN, 0.93F, 0.68F, 5, 145);
    private static final Voice POPE = new Voice(ModSounds.DIALOGUE_VOICE_POPE, 0.82F, 0.70F, 5, 145);
    private static final Voice GENERIC_MALE = new Voice(ModSounds.DIALOGUE_VOICE_MALE, 0.96F, 0.72F, 4, 125);
    private static final Voice GENERIC_FEMALE = new Voice(ModSounds.DIALOGUE_VOICE_FEMALE, 1.04F, 0.72F, 4, 125);

    private DialogueVoiceBlips() { }

    /** Returns true if a grunt actually played. Screen stores last played time/index. */
    public static boolean play(DialogueView view, String speech,
                               int previousCount, int count,
                               int lastPlayedIndex, long now, long lastPlayedMillis) {
        if (!NpcVoiceConfig.ENABLED.get() || NpcVoiceConfig.VOLUME.get() <= 0D
                || count <= previousCount || speech.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        if (!(mc.level.getEntity(view.entityId()) instanceof CyberNpcEntity npc)) return false;

        Voice voice = resolve(npc);
        if (count - lastPlayedIndex < voice.letters()
                || now - lastPlayedMillis < voice.minMillis()) return false;
        int i = Math.min(count, speech.length()) - 1;
        if (i < 0 || !Character.isLetterOrDigit(speech.charAt(i))) return false;

        // Vary pitch subtly between grunts, but give each NPC a consistent tone.
        int variation = (i * 31 + speech.length() * 7 + npc.getId() * 13) & 7;
        float pitch = voice.pitch() * (1.0F + (variation - 3.5F) * 0.011F);
        if (isGeneric(voice)) {
            // Stable per-entity personality for future authored dialogue NPCs.
            pitch *= 1.0F + (Math.floorMod(npc.getUUID().hashCode(), 9) - 4) * 0.018F;
        }
        float volume = (float) (NpcVoiceConfig.VOLUME.get() * voice.gain());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(voice.sample().get(), pitch, volume));
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
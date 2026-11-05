package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(
                    ForgeRegistries.SOUND_EVENTS,
                    CyberNpc.MOD_ID
            );

    public static final RegistryObject<SoundEvent> COURIER_BREATHING =
            SOUND_EVENTS.register(
                    "courier_breathing",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(
                                    CyberNpc.MOD_ID,
                                    "courier_breathing"
                            )
                    )
            );

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_MALE =
            registerDialogueVoice("dialogue_voice_male");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_FEMALE =
            registerDialogueVoice("dialogue_voice_female");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_KAI =
            registerDialogueVoice("dialogue_voice_kai");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_KATIE =
            registerDialogueVoice("dialogue_voice_katie");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_ELARA =
            registerDialogueVoice("dialogue_voice_elara");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_ROWAN =
            registerDialogueVoice("dialogue_voice_rowan");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_FELIX =
            registerDialogueVoice("dialogue_voice_felix");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_NADIA =
            registerDialogueVoice("dialogue_voice_nadia");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_ORIN =
            registerDialogueVoice("dialogue_voice_orin");

    public static final RegistryObject<SoundEvent> DIALOGUE_VOICE_POPE =
            registerDialogueVoice("dialogue_voice_pope");

    private static RegistryObject<SoundEvent> registerDialogueVoice(String key) {
        return SOUND_EVENTS.register(key, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(CyberNpc.MOD_ID, key)));
    }

    private ModSounds() {
    }
}

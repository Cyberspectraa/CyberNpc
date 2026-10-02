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

    private ModSounds() {
    }
}

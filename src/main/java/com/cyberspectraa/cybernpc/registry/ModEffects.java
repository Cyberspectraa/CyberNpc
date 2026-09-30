package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.effect.ZombificationEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CyberNpc.MOD_ID);

    public static final RegistryObject<MobEffect> ZOMBIFICATION =
            MOB_EFFECTS.register("zombification", ZombificationEffect::new);

    private ModEffects() {
    }
}

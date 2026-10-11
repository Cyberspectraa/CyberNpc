package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Client-only, optional CyberRaces skin bridge. Older CyberRaces JARs or
 * modpacks without CyberRaces keep CyberNpc's original skin and existing
 * rendering behavior. No shared/server classes ever reference client APIs.
 */
final class CyberNpcSlimeSkinCompat {
    private static boolean resolved;
    private static Method slimeBaseTexture;

    private CyberNpcSlimeSkinCompat() {}

    static ResourceLocation textureFor(CyberNpcEntity npc, ResourceLocation original) {
        if (!ModList.get().isLoaded("cyberraces")) return original;
        if (!resolved) resolveBridge();
        if (slimeBaseTexture == null) return original;

        try {
            Object value = slimeBaseTexture.invoke(null, npc, original);
            return value instanceof ResourceLocation result ? result : original;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return original;
        }
    }

    private static void resolveBridge() {
        resolved = true;
        try {
            Class<?> skinManager = Class.forName(
                "com.cyberspectraa.cyberraces.client.RaceSkinOverlayManager"
            );
            slimeBaseTexture = skinManager.getMethod(
                "getNpcSlimeBaseTexture",
                LivingEntity.class,
                ResourceLocation.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            slimeBaseTexture = null;
        }
    }
}

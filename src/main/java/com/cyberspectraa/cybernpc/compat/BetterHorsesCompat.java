package com.cyberspectraa.cybernpc.compat;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

public final class BetterHorsesCompat {
    public static final String MOD_ID = "icys_better_horses";

    private static boolean attemptedInit;
    private static boolean ready;
    private static Method ofMethod;
    private static Method getOwnerMethod;
    private static Method hasCartGearMethod;

    private BetterHorsesCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static UUID getBetterHorsesOwner(AbstractHorse horse) {
        if (horse == null || !ensureReady()) {
            return null;
        }

        try {
            Object data = ofMethod.invoke(null, horse);
            Object result = getOwnerMethod.invoke(data);
            return result instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static boolean hasCartGear(AbstractHorse horse) {
        if (horse == null || !ensureReady()) {
            return false;
        }

        try {
            Object data = ofMethod.invoke(null, horse);
            Object result = hasCartGearMethod.invoke(data);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean ensureReady() {
        if (!isLoaded()) {
            return false;
        }

        if (attemptedInit) {
            return ready;
        }

        attemptedInit = true;

        try {
            Class<?> dataClass = Class.forName(
                    "icy.betterhorses.net.IHorseData"
            );

            ofMethod = dataClass.getMethod(
                    "of",
                    AbstractHorse.class
            );
            getOwnerMethod = dataClass.getMethod("bh_getOwner");
            hasCartGearMethod = dataClass.getMethod("bh_hasCartGear");

            ready = true;
        } catch (ReflectiveOperationException ignored) {
            ready = false;
        }

        return ready;
    }
}

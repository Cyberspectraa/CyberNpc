package com.cyberspectraa.cybernpc.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

public final class BetterHorsesCompat {
    public static final String MOD_ID = "icys_better_horses";
    public static final String RESOURCE_NAMESPACE = "icys-better-horses";
    private static final ResourceLocation UPGRADED_SADDLE_ID =
            new ResourceLocation(RESOURCE_NAMESPACE, "upgraded_saddle");

    private static boolean attemptedInit;
    private static boolean ready;
    private static Method ofMethod;
    private static Method getOwnerMethod;
    private static Method setOwnerMethod;
    private static Method hasCartGearMethod;
    private static Method hasUpgradedSaddleMethod;
    private static Method equipUpgradedSaddleMethod;

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

    public static boolean hasUpgradedSaddle(AbstractHorse horse) {
        if (horse == null || !ensureReady()) {
            return false;
        }

        try {
            Object data = ofMethod.invoke(null, horse);
            Object result = hasUpgradedSaddleMethod.invoke(data);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean setBetterHorsesOwner(
            AbstractHorse horse,
            @Nullable UUID owner
    ) {
        if (horse == null || !ensureReady()) {
            return false;
        }

        try {
            Object data = ofMethod.invoke(null, horse);
            setOwnerMethod.invoke(data, owner);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean equipUpgradedSaddle(
            AbstractHorse horse,
            ItemStack saddle
    ) {
        if (horse == null
                || saddle == null
                || saddle.isEmpty()
                || !isUpgradedSaddle(saddle)
                || !ensureReady()) {
            return false;
        }

        try {
            Object data = ofMethod.invoke(null, horse);
            equipUpgradedSaddleMethod.invoke(
                    data,
                    saddle.copyWithCount(1)
            );
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static ItemStack createUpgradedSaddle() {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(UPGRADED_SADDLE_ID);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    public static boolean isUpgradedSaddle(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isLoaded()) {
            return false;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return UPGRADED_SADDLE_ID.equals(key);
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
            setOwnerMethod = dataClass.getMethod("bh_setOwner", UUID.class);
            hasCartGearMethod = dataClass.getMethod("bh_hasCartGear");
            hasUpgradedSaddleMethod = dataClass.getMethod("bh_hasUpgradedSaddle");
            equipUpgradedSaddleMethod = dataClass.getMethod(
                    "bh_equipUpgradedSaddle",
                    ItemStack.class
            );

            ready = true;
        } catch (ReflectiveOperationException ignored) {
            ready = false;
        }

        return ready;
    }
}

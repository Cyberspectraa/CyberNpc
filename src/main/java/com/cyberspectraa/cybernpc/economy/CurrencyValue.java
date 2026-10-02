package com.cyberspectraa.cybernpc.economy;

import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.world.item.ItemStack;

public final class CurrencyValue {
    public static final long COPPER = 1L;
    public static final long SILVER = 10L;
    public static final long GOLD = 100L;
    public static final long PLATINUM = 1_000L;
    public static final long DRAGON = 10_000L;

    public static long valueOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }

        return unitValueOf(stack) * stack.getCount();
    }

    public static long unitValueOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }

        if (stack.is(ModItems.COPPER_COIN.get())) {
            return COPPER;
        }
        if (stack.is(ModItems.SILVER_COIN.get())) {
            return SILVER;
        }
        if (stack.is(ModItems.GOLD_COIN.get())) {
            return GOLD;
        }
        if (stack.is(ModItems.PLATINUM_COIN.get())) {
            return PLATINUM;
        }
        if (stack.is(ModItems.DRAGON_COIN.get())) {
            return DRAGON;
        }

        return 0L;
    }

    public static boolean isCurrency(ItemStack stack) {
        return valueOf(stack) > 0L;
    }

    public static String format(long value) {
        return String.format("%,d", Math.max(0L, value));
    }

    private CurrencyValue() {
    }
}

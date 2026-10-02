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

        long perItem;

        if (stack.is(ModItems.COPPER_COIN.get())) {
            perItem = COPPER;
        } else if (stack.is(ModItems.SILVER_COIN.get())) {
            perItem = SILVER;
        } else if (stack.is(ModItems.GOLD_COIN.get())) {
            perItem = GOLD;
        } else if (stack.is(ModItems.PLATINUM_COIN.get())) {
            perItem = PLATINUM;
        } else if (stack.is(ModItems.DRAGON_COIN.get())) {
            perItem = DRAGON;
        } else {
            return 0L;
        }

        return perItem * stack.getCount();
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

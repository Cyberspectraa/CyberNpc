package com.cyberspectraa.cybernpc.economy;

import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    public static List<ItemStack> makePayout(long amount) {
        long remaining = Math.max(0L, amount);
        List<ItemStack> result = new ArrayList<>();

        remaining = addPayout(
                result,
                ModItems.DRAGON_COIN.get(),
                DRAGON,
                remaining
        );
        remaining = addPayout(
                result,
                ModItems.PLATINUM_COIN.get(),
                PLATINUM,
                remaining
        );
        remaining = addPayout(
                result,
                ModItems.GOLD_COIN.get(),
                GOLD,
                remaining
        );
        remaining = addPayout(
                result,
                ModItems.SILVER_COIN.get(),
                SILVER,
                remaining
        );
        addPayout(
                result,
                ModItems.COPPER_COIN.get(),
                COPPER,
                remaining
        );

        return result;
    }

    private static long addPayout(
            List<ItemStack> result,
            Item item,
            long unitValue,
            long remaining
    ) {
        long count = remaining / unitValue;
        remaining %= unitValue;

        while (count > 0L) {
            int stackSize = (int) Math.min(64L, count);
            result.add(new ItemStack(item, stackSize));
            count -= stackSize;
        }

        return remaining;
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

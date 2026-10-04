package com.cyberspectraa.cybernpc.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

public final class EpicKnightsCompat {
    public static final String MOD_ID = "magistuarmory";

    private static final String[] GUARD_WEAPONS = new String[]{
            "steel_shortsword",
            "steel_katzbalger",
            "iron_shortsword",
            "noble_sword",
            "messer_sword"
    };

    private static final String[] GUARD_SHIELDS = new String[]{
            "steel_heatershield",
            "steel_kiteshield",
            "iron_heatershield",
            "iron_kiteshield"
    };

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static GuardLoadout createGuardLoadout(
            RandomSource random
    ) {
        if (!isLoaded()) {
            return GuardLoadout.EMPTY;
        }

        return new GuardLoadout(
                randomFromIds(random, GUARD_WEAPONS),
                randomFromIds(random, GUARD_SHIELDS),
                item("armet"),
                item("knight_chestplate"),
                item("knight_leggings"),
                item("knight_boots")
        );
    }

    private static ItemStack randomFromIds(
            RandomSource random,
            String[] ids
    ) {
        if (random == null || ids.length == 0) {
            return ItemStack.EMPTY;
        }

        int start = random.nextInt(ids.length);

        for (int offset = 0; offset < ids.length; offset++) {
            ItemStack stack = item(
                    ids[(start + offset) % ids.length]
            );
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack item(String path) {
        Item item = BuiltInRegistries.ITEM
                .getOptional(new ResourceLocation(MOD_ID, path))
                .orElse(Items.AIR);

        return item == Items.AIR
                ? ItemStack.EMPTY
                : new ItemStack(item);
    }

    public record GuardLoadout(
            ItemStack weapon,
            ItemStack shield,
            ItemStack helmet,
            ItemStack chestplate,
            ItemStack leggings,
            ItemStack boots
    ) {
        private static final GuardLoadout EMPTY =
                new GuardLoadout(
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY
                );
    }

    private EpicKnightsCompat() {
    }
}

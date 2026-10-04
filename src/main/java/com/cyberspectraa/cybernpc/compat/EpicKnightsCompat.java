package com.cyberspectraa.cybernpc.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class EpicKnightsCompat {
    public static final String MOD_ID = "magistuarmory";

    private EpicKnightsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static GuardGear createGuardGear() {
        if (!isLoaded()) {
            return GuardGear.empty();
        }

        // Short sword is deliberately used rather than a two-handed weapon so
        // the guard can keep a medieval shield in the offhand.
        ItemStack weapon = item("iron_shortsword");
        ItemStack shield = firstPresent(
                "iron_heatershield",
                "iron_kiteshield",
                "iron_roundshield",
                "iron_target"
        );

        ItemStack helmet = item("armet");
        ItemStack chest = item("knight_chestplate");
        ItemStack legs = item("knight_leggings");
        ItemStack boots = item("knight_boots");

        if (weapon.isEmpty()
                || shield.isEmpty()
                || helmet.isEmpty()
                || chest.isEmpty()
                || legs.isEmpty()
                || boots.isEmpty()) {
            return GuardGear.empty();
        }

        return new GuardGear(
                weapon,
                shield,
                helmet,
                chest,
                legs,
                boots
        );
    }

    private static ItemStack firstPresent(String... paths) {
        for (String path : paths) {
            ItemStack stack = item(path);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack item(String path) {
        Item item = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(MOD_ID, path)
        );
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public record GuardGear(
            ItemStack weapon,
            ItemStack shield,
            ItemStack helmet,
            ItemStack chest,
            ItemStack legs,
            ItemStack boots
    ) {
        private static GuardGear empty() {
            return new GuardGear(
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
            );
        }

        public boolean complete() {
            return !weapon.isEmpty()
                    && !shield.isEmpty()
                    && !helmet.isEmpty()
                    && !chest.isEmpty()
                    && !legs.isEmpty()
                    && !boots.isEmpty();
        }
    }
}

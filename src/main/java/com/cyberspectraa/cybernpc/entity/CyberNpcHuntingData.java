package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CyberNpcHuntingData {
    public static final TagKey<EntityType<?>> WILD_NPC_PREY =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(CyberNpc.MOD_ID, "wild_npc_prey"));

    public static final TagKey<Item> WILD_NPC_RAW_FOOD =
            TagKey.create(Registries.ITEM, new ResourceLocation(CyberNpc.MOD_ID, "wild_npc_raw_food"));

    public static boolean isRawFood(ItemStack stack) {
        return !stack.isEmpty() && stack.is(WILD_NPC_RAW_FOOD);
    }

    public static ItemStack cookOne(ItemStack raw) {
        Item item = raw.getItem();

        if (item == Items.BEEF) {
            return new ItemStack(Items.COOKED_BEEF);
        }
        if (item == Items.PORKCHOP) {
            return new ItemStack(Items.COOKED_PORKCHOP);
        }
        if (item == Items.CHICKEN) {
            return new ItemStack(Items.COOKED_CHICKEN);
        }
        if (item == Items.MUTTON) {
            return new ItemStack(Items.COOKED_MUTTON);
        }
        if (item == Items.RABBIT) {
            return new ItemStack(Items.COOKED_RABBIT);
        }

        return ItemStack.EMPTY;
    }

    public static int hungerRestored(ItemStack cooked) {
        Item item = cooked.getItem();

        if (item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP) {
            return 8;
        }
        if (item == Items.COOKED_CHICKEN || item == Items.COOKED_MUTTON) {
            return 6;
        }
        if (item == Items.COOKED_RABBIT) {
            return 5;
        }

        return 0;
    }

    private CyberNpcHuntingData() {
    }
}
